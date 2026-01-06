package net.judah.seq;

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

/** Utility to pair note on/off events into MidiNote wrappers. */
public final class NotePairer {
    private NotePairer() {}

    /**
     * Pair note-on and note-off events in a Track.
     * Returns a list of MidiNote objects (on + optional off) in track order.
     */
    public static List<MidiNote> pair(Track t) {
        List<MidiNote> result = new ArrayList<>();
        // Map key: channel<<8 | data1  -> stack of MidiEvent (note-on)
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
                if (on != null) {
                    result.add(new MidiNote(on, e)); // paired
                } else {
                    // orphan note-off: represent as MidiNote with null on, or skip
                    result.add(new MidiNote(e)); // if MidiNote has ctor for single event
                }
            } else {
                // non-note events ignored by this helper
            }
        }
        // remaining open note-ons -> unclosed MidiNote (off == null)
        for (Deque<MidiEvent> stack : open.values()) {
            while (!stack.isEmpty()) {
                MidiEvent on = stack.removeLast(); // preserve original order
                result.add(new MidiNote(on)); // off == null
            }
        }
        // sort by on-tick to preserve track order
        result.sort(Comparator.comparingLong(MidiNote::getTick));
        return result;
    }
}
