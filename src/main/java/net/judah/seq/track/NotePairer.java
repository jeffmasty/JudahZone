package net.judah.seq.track;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

import judahzone.api.Midi;

/** Utility to pair note on/off events and return an interleaved List\<MidiEvent\>. */
public final class NotePairer {
    private NotePairer() {}

    private static record Pair(MidiEvent on, MidiEvent off) {}

    public static MidiEvent getOff(MidiEvent on, Track t) {
        if (on == null || on.getMessage() instanceof ShortMessage == false)
            return null;
        ShortMessage onMsg = (ShortMessage) on.getMessage();
        final int data1 = onMsg.getData1();
        final int channel = onMsg.getChannel();

        // start searching strictly after the on tick
        int idx = MidiTools.find(t, on.getTick() + 1);
        if (idx < 0) return null;

        for (int i = idx; i < t.size(); i++) {
            MidiEvent e = t.get(i);
            MidiMessage m = e.getMessage();
            if (!(m instanceof ShortMessage)) continue;
            ShortMessage sm = (ShortMessage) m;
            if (!Midi.isNoteOff(sm)) continue;
            if (sm.getChannel() == channel && (sm.getData1() & 0xff) == (data1 & 0xff))
                return e;
        }
        return null;
    }
    /**
     * Pair note-on and note-off events in a Track.
     * Returns a List of MidiEvent where each paired note appears as [on, off].
     * Orphan note-offs or unclosed note-ons are included as single events.
     */
    public static List<MidiEvent> pair(Track t) {
        List<Pair> pairs = new ArrayList<>();
        // key: channel<<8 | data1  -> stack of note-on MidiEvent
        Map<Integer, Deque<MidiEvent>> open = new HashMap<>();

        for (int i = 0; i < t.size(); i++) {
            MidiEvent e = t.get(i);
            MidiMessage m = e.getMessage();
            if (!(m instanceof ShortMessage)) continue;
            ShortMessage sm = (ShortMessage) m;
            if (Midi.isNoteOn(sm)) {
                int key = (sm.getChannel() << 8) | (sm.getData1() & 0xff);
                open.computeIfAbsent(key, k -> new ArrayDeque<>()).push(e);
            } else if (Midi.isNoteOff(sm)) {
                int key = (sm.getChannel() << 8) | (sm.getData1() & 0xff);
                Deque<MidiEvent> stack = open.get(key);
                MidiEvent on = (stack == null || stack.isEmpty()) ? null : stack.pop();
                pairs.add(new Pair(on, e));
            }
        }

        // Any remaining open note-ons -> unclosed pairs (off == null).
        for (Deque<MidiEvent> stack : open.values()) {
            // removeLast to preserve original on-event order
            while (!stack.isEmpty()) {
                MidiEvent on = stack.removeLast();
                pairs.add(new Pair(on, null));
            }
        }

        // Sort pairs by on-tick if present, otherwise by off-tick
        pairs.sort(Comparator.comparingLong(p -> {
            if (p.on() != null) return p.on().getTick();
            if (p.off() != null) return p.off().getTick();
            return Long.MAX_VALUE;
        }));

        // Flatten to interleaved MidiEvent list: on then off (if present)
        List<MidiEvent> result = new ArrayList<>(pairs.size() * 2);
        for (Pair p : pairs) {
            if (p.on() != null) result.add(p.on());
            if (p.off() != null) result.add(p.off());
        }

        return result;
    }
}
