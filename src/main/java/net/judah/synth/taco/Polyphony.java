package net.judah.synth.taco;

import javax.sound.midi.ShortMessage;

import judahzone.api.Midi;
import judahzone.prism.MidiCmdPack;
import judahzone.prism.PrismUI;
import judahzone.util.RTLogger;
import net.judah.gui.MainFrame;
import net.judah.midi.Actives;
import net.judah.synth.Engine;

/** Polyphony: UI-facing container of active notes plus voice slots. */
public class Polyphony extends Actives {

    protected final int polyphony;
    public final Voice[] voices;

    public Polyphony(Engine out, int ch) {
        this(out, ch, TacoSynth.POLYPHONY);
    }

    public Polyphony(Engine out, int ch, int polyphony) {
        super(out, ch);
        this.polyphony = polyphony;
        this.voices = new Voice[polyphony];
        // Pre-fill active list to maintain stable indices matching voices array
        for (int i = 0; i < polyphony; i++)
            active.add(null);
    }

    /** Find or assign a voice slot for a Note On event. (UI thread) */
    public int add(ShortMessage msg) {
        int d1 = msg.getData1();
        int d2 = msg.getData2();

        ShortMessage copy = Midi.copy(msg);

        // 1. Search for existing voice (prevent duplicate notes)
        for (int i = 0; i < voices.length; i++) {
            if (voices[i].isAllocated() && voices[i].getData1() == d1) {
                voices[i].reset(d1, d2);
                active.set(i, copy);
                return i;
            }
        }

        // 2. Find available slot
        for (int i = 0; i < voices.length; i++) {
            if (!voices[i].isAllocated()) {
                voices[i].reset(d1, d2);
                active.set(i, copy);
                return i;
            }
        }

        RTLogger.log(this, "Polyphony limit reached, ignoring " + Midi.toString(msg));
        return -1;
    }

    /** Check if a note is currently held down (not in release). */
    public boolean isNoteOn(int data1) {
        synchronized (active) {
            for (ShortMessage m : active)
                if (m != null && m.getCommand() == ShortMessage.NOTE_ON && m.getData1() == data1)
                    return true;
        }
        return false;
    }

    @Override
    protected boolean noteOff(ShortMessage m) {
        int target = m.getData1();
        synchronized (active) {
            for (int i = 0; i < active.size(); i++) {
                ShortMessage midi = active.get(i);
                if (midi != null && midi.getData1() == target && midi.getCommand() == ShortMessage.NOTE_ON) {
                    if (pedal) {
                        if (susOf(target) < 0)
                            sustained.add(midi);
                    } else {
                        // Change status to NOTE_OFF so Voice can enter release stage
                        active.set(i, Midi.create(ShortMessage.NOTE_OFF, channel, target, midi.getData2()));
                    }
                    MainFrame.update(this);
                    return true;
                }
            }
        }
        return false;
    }

    @Override @PrismUI
    public synchronized void update() {
        int packed;
        while ((packed = rtToGui.poll()) != Integer.MIN_VALUE) {
            int cmd = MidiCmdPack.cmdType(packed);
            int d1 = MidiCmdPack.data1(packed);
            int d2 = MidiCmdPack.data2(packed);

            switch (cmd) {
            case MidiCmdPack.CMD_ADD, MidiCmdPack.CMD_RETRIGGER ->
                add(Midi.create(ShortMessage.NOTE_ON, channel, d1, d2));
            case MidiCmdPack.CMD_NOTE_OFF ->
                noteOff(Midi.create(ShortMessage.NOTE_OFF, channel, d1, 0));
            case MidiCmdPack.CMD_REMOVE -> {
                for(int i=0; i<voices.length; i++)
                    if(voices[i].getData1() == d1) active.set(i, null);
            }
            }
        }
        updateSnapshot();
    }

    @Override
    protected synchronized void updateSnapshot() {
        // Only snapshot Data1 for fast RT check of "is this note active at all"
        int count = 0;
        for (ShortMessage m : active) if (m != null) count++;
        int[] snap = new int[count];
        int idx = 0;
        for (ShortMessage m : active)
            if (m != null) snap[idx++] = m.getData1();
        activeSnapshot = snap;
    }
}
