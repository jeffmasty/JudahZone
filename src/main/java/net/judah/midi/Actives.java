package net.judah.midi;

import java.util.ArrayList;
import java.util.Set;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.ShortMessage;

import judahzone.api.Midi;
import judahzone.api.RTNotes;
import judahzone.api.UINotes;
import judahzone.gui.Updateable;
import judahzone.prism.IntRing;
import judahzone.prism.MidiCmdPack;
import judahzone.prism.PrismRT;
import judahzone.prism.PrismUI;
import judahzone.util.RTLogger;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.judah.gui.MainFrame;
import net.judah.synth.ZoneMidi;

/** Listens to and tracks note_on and note_off on a MIDI port.
    Provides RT-safe snapshot reads via isActive() and deferred mutations
    via RT→GUI lock-free command ring (IntRing). */
@RequiredArgsConstructor @Getter
public class Actives implements RTNotes, UINotes, Updateable {

protected final ZoneMidi midiOut;
protected final int channel;
protected boolean pedal;

protected final IntRing rtToGui = new IntRing(256); // RT producer → UI consumer

protected final ArrayList<ShortMessage> active = new ArrayList<>(); // UI-owned mutable list
protected final ArrayList<ShortMessage> sustained = new ArrayList<>(); // pedal-held notes

protected volatile int[] activeSnapshot = new int[0]; // RT-safe snapshot (primitives, no allocs)

// ======== RT-side (allocation-free, primitive) ========

/** RT-safe: receive packed MIDI command (no allocations). */
@PrismRT @Override
public void receivePacked(int packedMidiCmd) {
    int cmd = MidiCmdPack.cmdType(packedMidiCmd);
    int data1 = MidiCmdPack.data1(packedMidiCmd);

    if (cmd == MidiCmdPack.CMD_ADD || cmd == MidiCmdPack.CMD_RETRIGGER) {
        if (pedal && indexOf(data1) >= 0) {
            // re-trigger during pedal: queue retrigger command
            rtToGui.offer(packedMidiCmd);
        }
        // queue ADD or RETRIGGER
        rtToGui.offer(packedMidiCmd);
    }
    if (cmd == MidiCmdPack.CMD_NOTE_OFF) {
        // queue NOTE_OFF; pedal logic handled in UI
        rtToGui.offer(packedMidiCmd);
    }
    MainFrame.update(this);
}


@PrismRT @Override
public void removeData1(int data1) {
    // Use the explicit helper to avoid argument-order mistakes
    rtToGui.offer(MidiCmdPack.packRemove(data1));
}

/** RT-safe: query snapshot (no locks, no allocations). */
@PrismRT @Override
public boolean isActive(int data1) {
    int[] snap = activeSnapshot;
    for (int d : snap)
        if (d == data1) return true;
    return false;
}

/** RT-safe: index lookup in snapshot. */
@PrismRT @Override
public int indexOf(int data1) {
    int[] snap = activeSnapshot;
    for (int i = 0; i < snap.length; i++)
        if (snap[i] == data1) return i;
    return -1;
}

// ======== UI-side (allocations OK, mutations happen here) ========

/** UI-safe: receive full ShortMessage (allocations acceptable). */
@Override @PrismUI
public synchronized boolean receive(ShortMessage msg) {
    if (Midi.isNoteOn(msg)) {
        int d1 = msg.getData1();
        int d2 = msg.getData2();
        int cmd = (pedal && indexOf(d1) >= 0)
            ? MidiCmdPack.CMD_RETRIGGER
            : MidiCmdPack.CMD_ADD;
        return rtToGui.offer(MidiCmdPack.pack(cmd, d1, d2, channel));
    }
    if (Midi.isNoteOff(msg))
        return noteOff(msg);
    return true;
}

/** UI-safe: fill ref with current active data1 values. */
@Override @PrismUI
public synchronized void data1(Set<Integer> ref) {
    ref.clear();
    for (ShortMessage m : active)
        if (m != null)
            ref.add(m.getData1());
}

/** UI-safe: engage or release foot pedal (CC64). */
@Override @PrismUI
public synchronized void setPedal(boolean pressed) {
    pedal = pressed;
    if (pedal)
        return;
    try {
        for (ShortMessage sus : sustained)
            JudahMidi.queue(new ShortMessage(ShortMessage.NOTE_OFF, channel,
                sus.getData1(), 0), ((MidiInstrument)midiOut).getMidiPort());
        sustained.clear();
    } catch (InvalidMidiDataException e) {
        RTLogger.warn(this, e);
    }
}

/** UI-safe: drain RT→GUI ring and apply mutations. */
@Override @PrismUI
public synchronized void update() {
    int packed;
    while ((packed = rtToGui.poll()) != Integer.MIN_VALUE) {
        int cmd = MidiCmdPack.cmdType(packed);
        int data1 = MidiCmdPack.data1(packed);
        int data2 = MidiCmdPack.data2(packed);

        switch (cmd) {
        case MidiCmdPack.CMD_ADD:
            addNote(data1, data2);
            break;
        case MidiCmdPack.CMD_RETRIGGER:
            retrigger(data1, data2);
            break;
        case MidiCmdPack.CMD_NOTE_OFF:
            removeData1(data1);
            break;
        }
    }
    updateSnapshot();
}

/** Add or replace a note (UI thread). */
private synchronized void addNote(int data1, int data2) {
    int idx = -1;
    for (int i = 0; i < active.size(); i++) {
        ShortMessage m = active.get(i);
        if (m != null && m.getData1() == data1) { idx = i; break; }
    }
    try {
        if (idx >= 0) {
            active.set(idx, new ShortMessage(ShortMessage.NOTE_ON, channel, data1, data2));
        } else {
            active.add(new ShortMessage(ShortMessage.NOTE_ON, channel, data1, data2));
        }
    } catch (InvalidMidiDataException e) {
        RTLogger.warn(this, e);
    }
}

/** Re-trigger: send NOTE_OFF then add fresh (UI thread). */
private synchronized void retrigger(int data1, int data2) {
    try {
        ((MidiInstrument)midiOut).write(new ShortMessage(ShortMessage.NOTE_OFF, channel, data1, 0));
        addNote(data1, data2);
    } catch (InvalidMidiDataException e) {
        RTLogger.warn(this, e);
    }
}

/** Remove note by data1 (UI thread). */
private synchronized void removeNoteUI(int data1) {
    for (int i = 0; i < active.size(); i++) {
        ShortMessage m = active.get(i);
        if (m != null && m.getData1() == data1) {
            active.remove(i);
            return;
        }
    }
}

/** Note-off with pedal support (UI thread). */
protected synchronized boolean noteOff(ShortMessage msg) {
    int data1 = msg.getData1();
    if (pedal) {
        if (susOf(data1) < 0)
            try {
                sustained.add(new ShortMessage(ShortMessage.NOTE_OFF, channel, data1, 0));
            } catch (InvalidMidiDataException e) {
                RTLogger.warn(this, e);
            }
        return false;
    }
    removeNoteUI(data1);
    MainFrame.update(this);
    return true;
}

/** Rebuild snapshot from active list (UI thread, called from update()). */
protected synchronized void updateSnapshot() {
    int[] snap = new int[active.size()];
    int count = 0;
    for (ShortMessage m : active)
        if (m != null)
            snap[count++] = m.getData1();
    if (count < snap.length) {
        int[] trimmed = new int[count];
        System.arraycopy(snap, 0, trimmed, 0, count);
        snap = trimmed;
    }
    activeSnapshot = snap; // volatile publish
}

// ======== Utility queries ========

public boolean isEmpty() {
    return active.isEmpty();
}

public int size() {
    return active.size();
}

public ShortMessage get(int idx) {
    return active.get(idx);
}

public ShortMessage find(int data1) {
    int idx = indexOf(data1);
    return idx >= 0 ? active.get(idx) : null;
}

public void removeIndex(int idx) {
    if (idx >= 0 && idx < activeSnapshot.length)
        removeNoteUI(activeSnapshot[idx]);
}

protected int susOf(int data1) {
    for (int i = 0; i < sustained.size(); i++)
        if (sustained.get(i).getData1() == data1)
            return i;
    return -1;
}

}