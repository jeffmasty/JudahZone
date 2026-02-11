package net.judah.seq.track;

import java.util.Arrays;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

import judahzone.api.Key;
import judahzone.api.Midi;
import judahzone.util.RTLogger;
import net.judah.seq.Meta;

public class MidiTools {

	/**Find the index of the first event whose tick is >= the given tick
	 * using a classic binary search (lower_bound).
	 * @param t    The MIDI track (assumed sorted by event tick).
	 * @param tick The target tick.
	 * @return The index of the first event with getTick() >= tick, or -1 if none.
	 */
	public static int find(Track t, long tick) {
	    int n = t.size();
	    if (n == 0) {
	        return -1;
	    }

	    int lo = 0;
	    int hi = n; // half-open interval [lo, hi)

	    while (lo < hi) {
	        int mid = (lo + hi) >>> 1; // avoid overflow
	        long midTick = t.get(mid).getTick();

	        if (midTick < tick) {
	            lo = mid + 1; // candidate must be right of mid
	        } else {
	            hi = mid;     // mid might be the answer; shrink from the right
	        }
	    }

	    // lo == hi is the first index where getTick() >= tick (or n if none)
	    if (lo >= n) {
	        return -1;
	    }
	    return lo;
	}

    public static long wrapTickInWindow(long tick, long start, long window) {
        if (tick < start) tick += window;
        if (tick >= start + window) tick -= window;
        return tick;
    }

	/**
	 * Compares two MidiMessage objects to see if they represent the same musical instruction,
	 * ignoring mutable details like velocity.
	 * @param a The first message.
	 * @param b The second message.
	 * @return true if the messages are functionally identical.
	 */
	public static boolean messagesMatch(MidiMessage a, MidiMessage b) {
		if (a == b) return true;
		if (a == null || b == null) return false;

		// For ShortMessages, compare command, channel, and data1. Ignore data2 (velocity).
		if (a instanceof ShortMessage smA && b instanceof ShortMessage smB) {
			return smA.getCommand() == smB.getCommand() &&
				   smA.getChannel() == smB.getChannel() &&
				   smA.getData1() == smB.getData1();
		}
		// For MetaMessages, compare type and data content.
		if (a instanceof MetaMessage mmA && b instanceof MetaMessage mmB) {
			return mmA.getType() == mmB.getType() &&
				   Arrays.equals(mmA.getData(), mmB.getData());
		}
		// Fallback for other message types or direct byte comparison
		return Arrays.equals(a.getMessage(), b.getMessage());
	}


	/**Piano lookup */
	public static MidiEvent lookup(long tick, int data1, Track t) {
		MidiEvent found = null;
		for (int i = 0; i < t.size(); i++) {
			if (false == t.get(i).getMessage() instanceof ShortMessage)
				continue;
			MidiEvent e = t.get(i);
			ShortMessage m = (ShortMessage) e.getMessage();

			if (e.getTick() <= tick) {
				if (found == null) {
					if (Midi.isNoteOn(m) && m.getData1() == data1)
						found = e;
				}
				else if (found != null) {
					if (Midi.isNoteOff(m) && m.getData1() == data1)
						found = null;		}
			}
			else { // e.getTick() > tick
				if (found == null) // opportunity passed
					return null;
				if (Midi.isNoteOff(m) && m.getData1() == data1) {
					return new PianoNote(found, e);
				}
			}
		}
		return found;
//		if (found != null)
//			return new MidiNote(found);
//
//		return null;
	}

	public static MidiEvent lookup(int cmd, int data1, long tick, Track t) {
		int idx = find(t, tick);
		if (idx < 0)
			return null;
		for (; idx < t.size(); idx++) {
			if (t.get(idx).getTick() > tick)
				return null;
			if (t.get(idx).getMessage() instanceof ShortMessage m) {
				if (m.getCommand() == cmd && m.getData1() == data1)
					return t.get(idx);
			}
		}
		return null;
	}

	public static PianoNote noteOff(MidiEvent on, Track t) {
		final long fromTick = on.getTick();
		final int data1 = ((ShortMessage)on.getMessage()).getData1();
		for (int i = 0; i < t.size(); i++) {
			if (false == t.get(i).getMessage() instanceof ShortMessage)
				continue;
			MidiEvent e = t.get(i);
			ShortMessage m = (ShortMessage) e.getMessage();
			if (Midi.isNoteOff(m) && m.getData1() == data1) {

			if (e.getTick() <= fromTick)
				continue;
			if (Midi.isNoteOff(m) && m.getData1() == data1)
				return new PianoNote(on, e);

			}
		}
		return new PianoNote(on); // drums/hanging chad
	}

	public static boolean match(PianoNote it, PianoNote other) {
		return match(it, other) && match(it.getOff(), other.getOff());
	}
	public static boolean match(MidiEvent it, MidiEvent other) {
		if (it == null && other == null) return true;
		if (it == null || other == null) return false;
		if (it.getTick() != other.getTick()) return false;
		return messagesMatch(it.getMessage(), other.getMessage());
	}

	public static int measureCount(long ticks, long measureTicks) {
		return (int)Math.ceil(ticks / measureTicks) + 1;
	}

	/** @return true if delete was matched and removed from track */
	public static void delete(MidiEvent delete, Track t) {
		long tick = delete.getTick();
		int idx = find(t, tick);
		if (idx < 0) {
			RTLogger.log(MidiTools.class, "Missing: " + Midi.toString(delete.getMessage()) + " @ " + delete.getTick());
			return;
		}
		for (; idx < t.size(); idx++) {
			MidiEvent e = t.get(idx);
			if (e.getTick() > tick) {
				RTLogger.log(MidiTools.class, "Missing: " + Midi.toString(delete.getMessage()) + " @ " + delete.getTick());
				return;
			}
			if (messagesMatch(e.getMessage(), delete.getMessage())) {
				t.remove(e);
				return;
			}
		}
		RTLogger.log(MidiTools.class, "Missing: " + Midi.toString(delete.getMessage()) + " @ " + delete.getTick());
	}

	public static String formatTime(long timecode) {
		long millis = (long) (timecode * 1000.0 + 0.5);
		return String.format("%02d:%02d:%02d.%03d", millis / 60 / 60 / 1000,
				(millis / 60 / 1000) % 60, (millis / 1000) % 60, millis % 1000);
	}

	public static MidiEvent zeroBase(long left, MidiEvent e) {
		if (left == 0)
			return e;
		return new MidiEvent(e.getMessage(), e.getTick() - left);
	}

	public static PianoNote zeroBase(PianoNote p, long left) {
		if (left == 0)
			return p;
		return new PianoNote(zeroBase(left, p), p.getOff() == null ?
				null : zeroBase(left, p.getOff()));
	}

	public static void addTape(Track t, long start, long amount) {
		MidiEvent midi;
		for (int i = t.size() - 1; i > -1; i--) {
			midi = t.get(i);
			if (midi.getMessage() instanceof ShortMessage sht) {
				if (midi.getTick() < start)
					break;
				if (midi.getTick() == start && Midi.isNoteOff(sht))
					continue; // hanging chads
				midi.setTick(midi.getTick() + amount);
			}
			else
				continue;
		}
	}

	public static void removeTape(Track t, long target, long amount) {
		MidiEvent midi;
		for (int i = t.size() - 1; i > -1; i--) {
			if (t.get(i).getMessage() instanceof ShortMessage == false)
				continue;
			midi = t.get(i);
			if (midi.getTick() < target)
				break;
			midi.setTick(midi.getTick() - amount);
		}
	}

	public static void copy(Track source, Track destination) {
		for (int i = 0; i < source.size(); i++)
			destination.add(source.get(i));
	}

	public static void copy(MidiTrack source, Track destination) {
		copy(source.getT(), destination);
//		source.getMeta().publish(destination);  // TODO
	}

	public static MidiEvent meta(Meta type, byte[] bytes, long tick) throws InvalidMidiDataException {
		return new MidiEvent(new MetaMessage(type.type, bytes, bytes.length), tick);
	}

	/**Computes transposed piano note pair.
	 * @param in Source note pair
	 * @param destination Target pitch and tick offset
	 * @param t Track reference
	 * @return New MidiNote at computed position*/
	public static PianoNote compute(PianoNote in, Prototype destination, MidiTrack t) {
	    if (!(in.getMessage() instanceof ShortMessage))
	        return in;

	    MidiEvent on = transposePiano((ShortMessage)in.getMessage(),
	        in.getTick(), destination, t);
	    MidiEvent off = null;
	    if (in.getOff() != null)
	        off = transposePiano((ShortMessage)in.getOff().getMessage(),
	            in.getOff().getTick(), destination, t);
	    return new PianoNote(on, off);
	}

	/**Transposes a single piano MIDI event.
	 * @param source Source MIDI message
	 * @param sourceTick Source tick position
	 * @param destination Target pitch and tick offset
	 * @param t Track reference
	 * @return New MidiEvent at transposed position */
	static MidiEvent transposePiano(ShortMessage source, long sourceTick,
	        Prototype destination, MidiTrack t) {
	    long window = t.getWindow();
	    long start = t.getCurrent() * t.getBarTicks();
	    long tick = sourceTick + (destination.tick * t.getStepTicks());

	    if (tick < start) tick += window;
	    if (tick >= start + window) tick -= window;

	    int data1 = source.getData1() + destination.data1;
	    if (data1 < 0) data1 += Key.OCTAVE;
	    if (data1 > 127) data1 -= Key.OCTAVE;

	    return new MidiEvent(Midi.create(source.getCommand(), source.getChannel(),
	        data1, source.getData2()), tick);
	}

}