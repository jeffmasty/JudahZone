package net.judah.seq;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

import net.judah.midi.Midi;
import net.judah.seq.track.MidiTrack;

public class MidiTools {

	/**Optimized tick-based midi track look up
	 * @param t
	 * @param tick find first note on or after tick
	 * @return index of first note or -1
	 */
	public static int fastFind(Track t, long tick) {
		// estimate a good place to find the first note (on or after tick)
		int max = t.size();
		if (max <= 2)
			return -1;

		long length = t.ticks();
		if (length == 0)
			return -1;

		float ratio = tick / length;

		if (ratio < 0.1f) { // from beginning
			for (int i = 0; i < max; i++)
				if (t.get(i).getTick() >= tick)
					return i;
		}


		int guess = (int) (ratio * max) - 1;

		while (guess > max || t.get(guess).getTick() > tick)
			guess = (int) (guess * 0.5f);
		// from here
		for (int i = guess; i < max; i++) {
			if (t.get(i).getTick() >= tick)
				return i;
		}

		return -1;
	}

	public static boolean match(MidiMessage a, MidiMessage b) {
		if (a == null && b == null) return true;
		if (a == null || b == null) return false;
		if (a instanceof ShortMessage && b instanceof ShortMessage) {
			ShortMessage it = (ShortMessage)a;
			ShortMessage other = (ShortMessage)b;
			return it.getChannel() == other.getChannel() &&
				it.getCommand() == other.getCommand() &&
				it.getData1() == other.getData1();
		}
		return a.equals(b);
	}


	/**Piano lookup */
	public static MidiPair lookup(long tick, int data1, Track t) {
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
					return new MidiPair(found, e);
				}
			}
		}
		if (found != null)
			return new MidiPair(found, null);
		return null;
	}

	public static MidiEvent lookup(int cmd, int data1, long tick, Track t) {
		int idx = fastFind(t, tick);
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


	public static MidiPair noteOff(MidiEvent on, Track t) {
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
				return new MidiPair(on, e);

			}
		}
		return new MidiPair(on, null); // drums/hanging chad
	}

	public static boolean match(MidiPair it, MidiPair other) {
		return match(it.getOn(), other.getOn()) && match(it.getOff(), other.getOff());
	}
	public static boolean match(MidiEvent it, MidiEvent other) {
		if (it == null && other == null) return true;
		if (it == null || other == null) return false;
		if (it.getTick() != other.getTick()) return false;
		return match(it.getMessage(), other.getMessage());
	}

	public static int measureCount(long ticks, long measureTicks) {
		return (int)Math.ceil(ticks / measureTicks) + 1;
	}

	/** @return true if delete was matched and removed from track */
	public static boolean delete(MidiEvent delete, Track t) {
		long tick = delete.getTick();
		int idx = fastFind(t, tick);
		if (idx < 0)
			return false;
		for (; idx < t.size(); idx++) {
			MidiEvent e = t.get(idx);
			if (e.getTick() > tick) return false;
			if (match(e.getMessage(), delete.getMessage())) {
				t.remove(e);
				return true;
			}
		}
		return false;
	}

	public static String formatTime(long timecode) {
		long millis = (long) (timecode * 1000.0 + 0.5);
		return String.format("%02d:%02d:%02d.%03d", millis / 60 / 60 / 1000,
				(millis / 60 / 1000) % 60, (millis / 1000) % 60, millis % 1000);
	}

	public static MidiPair zeroBase(MidiPair p, long left) {
		if (left == 0)
			return p;
		return new MidiPair(new MidiEvent(p.getOn().getMessage(), p.getOn().getTick() - left),
				p.getOff() == null ? null : new MidiEvent(p.getOff().getMessage(), p.getOff().getTick() - left));
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

	public static void copy(MidiTrack source, Track destination) {
		Track t = source.getT();
		for (int i = 0; i < t.size(); i++)
			destination.add(t.get(i));
		source.getMeta().publish(destination);
	}

}

// public static int octave(ShortMessage m) { return m.getData1() / 12 - 2; }
//  private static final Pattern work = new Pattern();
//	/** loads one bar from measure of track into the supplied result, zero-basing the ticks*/
//	public static AbstractCollection<MidiEvent> loadMeasure(int measure, MidiTrack track, AbstractCollection<MidiEvent> result) {
//		long start = measure * track.getBarTicks();
//		long end = start + track.getBarTicks();
//		return loadSection(start, end, track.getT(), result);
//	}
//	/** copy/paste given measure to end of track */
//	public static void append(int measure, MidiTrack track) {
//		int target = measureCount(track.getT().ticks(), track.getBarTicks()) + 1;
//		long base = target * track.getBarTicks();
//		Track t = track.getT();
//		loadMeasure(measure, track, work);
//		for (MidiEvent e : work)
//			t.add(new MidiEvent(e.getMessage(), e.getTick() + base));
//	}
//	public static Note translate(MidiEvent e, MidiTrack track) {
//		float twoBar = 2 * track.getBarTicks();
//		if (e.getMessage() instanceof ShortMessage == false)
//			return null;
//		ShortMessage m = (ShortMessage)e.getMessage();
//		long translate = track.getCurrent() * track.getBarTicks();
//		if (e.getTick() >= translate && e.getTick() < translate + track.getBarTicks()) {
//			// translate relative to bar a
//			float top = (e.getTick() - translate) / twoBar;
//			return new Note(top, top, m.getData1(), m.getData2());
//		}
//		// translate relative to bar b
//		translate = track.getNext() * track.getBarTicks();
//		float top = (e.getTick() - translate) / twoBar;
//		return new Note(top, top, m.getData1(), m.getData2());
//	}
//	private static void collateDrums(Bar b, long barTicks, Measure result, boolean first) {
//		result.clear();
//		for (MidiEvent e : b)
//			result.add(new MidiPair(new MidiEvent(e.getMessage(), e.getTick() + (first ? 0 : barTicks)), null));
//	}
//	private void snipNotes(Bar bar, long start, long end, long translate, Measure result, Accumulator stash) {
//		float twoBar = 2 * barTicks;
//		for (MidiEvent e : bar) {
//			if (e.getTick() < start || e.getMessage() instanceof ShortMessage == false)
//				continue;
//			if (e.getTick() >= end)
//				return;
//			ShortMessage s = (ShortMessage)e.getMessage();
//			if (track.isDrums()) {
//				float top = (e.getTick() + translate) / twoBar;
//				result.add(new Note(top, top, s.getData1(), s.getData2()));
//			}
//			else if (s.getCommand() == NOTE_ON)
//				stash.add(e);
//			else if (s.getCommand() == NOTE_OFF) {
//				MidiEvent on = stash.get(s);
//				float top = on == null ? 0 : (on.getTick() + translate) / twoBar;
//				float bottom = (e.getTick() + translate) / twoBar;
//				int velocity = on == null ? 99 : ((ShortMessage)on.getMessage()).getData2();
//				result.add(new Note(top, bottom, s.getData1(), velocity));
//			}
//		}
//	}

//	/** move trailing midi back */
//	public static void deleteMeasure(MidiTrack t) {
//		long measure = MidiTools.measureTicks(t.getResolution());
//		long start = index * measure;
//		long end = start + measure;
//		boolean cleansed = false;
//		for (int i = 0; i < track.size(); i++) {
//			MidiEvent e = track.get(i);
//			if (e.getTick() < start)
//				continue;
//			if (cleansed)
//				e.setTick(e.getTick() - measure);
//			else if (e.getTick() < end)
//				track.remove(e);
//			else {
//				cleansed = true;
//				e.setTick(e.getTick() - measure);
//			}
//		}
//	}
//public static void copyMeasure(Track track, long start, long end, ArrayList<MidiEvent> result, long offset) {
//for (int i = 0; i < track.size(); i++) {
//	MidiEvent e = track.get(i);
//	if (e.getMessage() instanceof ShortMessage == false)
//		continue;
//	long tick = e.getTick();
//	if (tick < start)
//		continue;
//	if (tick < end || (tick == end && e.getMessage().getStatus() == NOTE_OFF))
//		result.add(new MidiEvent(e.getMessage(), e.getTick() - start + offset));
//	else
//		return;
//}
//}

//	/** find next CMD (i.e. note_off) starting from tick (i.e. note_on) or null*/
//	public static MidiEvent findNext(int cmd, long tick, int data1, Track track) {
//		for (int i = 0; i < track.size(); i++) {
//			MidiEvent e = track.get(i);
//			if (e.getTick() < tick)
//				continue;
//			if (e.getMessage().getStatus() == cmd)
//				return e;
//		}
//		return null;
//	}

//	public static void copy(Collection<MidiEvent> source, Snippet dest, long start) {
//		long end = start + dest.getLength();
//		Iterator<MidiEvent> it = source.iterator();
//		dest.clear();
//		while (it.hasNext()) {
//			MidiEvent e = it.next();
//			long tick = e.getTick();
//			if (tick <= start)
//				continue;
//			if (tick < end || (tick == end && e.getMessage().getStatus() == NOTE_OFF))
//				dest.add(new MidiEvent(e.getMessage(), tick - start));
//			else
//				return;
//		}
//	}

//	/**put notes in result from track for the range as specified in result */
//	public static void snippet(Snippet result, Track track) {
//		result.clear();
//
//		long start = result.getStart();
//		long end = result.getEnd();
//		for (int i = 0; i < track.size(); i++) {
//			long tick = track.get(i).getTick();
//			if (tick < start)
//				continue;
//			if (tick >= start && tick < end)
//				result.add(track.get(i));
//			else
//				break;
//		}
//	}

//	public MidiEvent findNext(int cmd, int data1, int ref, long fromTick) {
//		for (int i = ref; i < 4; i++) {
//			MidiTools.loadMeasure(scheduler.get(i), this, work);
//			for (MidiEvent e : work) {
//				if (e.getTick() < fromTick)
//					continue;
//				if (e.getMessage() instanceof ShortMessage && e.getMessage().getStatus() == cmd) {
//					if ( ((ShortMessage)e.getMessage()).getData1() == data1)
//						return e;
//				}
//			}
//			fromTick = 0; // search from start of next bar
//		}
//		return null;
//	}

//	// incoming pattern names
//	for (int i = 0; i < incoming.size(); i++) {
//		if (incoming.get(i).getMessage() instanceof MetaMessage) {
//        	MetaMessage m = (MetaMessage)incoming.get(i).getMessage();
//        	if (m.getMessage()[1] == NAME_STATUS) {
//        		// long tick = incoming.get(i).getTick() / 2; // why divide tick by 2??
//        		get((int) (incoming.get(i).getTick() / ticks)).setName(new String(m.getData()));}}}

//	byte[] nombre = b.getName().getBytes();
//	try { // save outgoing pattern names
//		t.add(new MidiEvent(new MetaMessage(NAME_STATUS, nombre, nombre.length), bar * ticks));
//	} catch (InvalidMidiDataException e) {

//    public abstract void next(boolean forward);
//    /** copy bar from to tail of track */
//    public abstract void copy(int from);
//    /** copy bar from, inserting at to */
//    public abstract void copy(int from, int to);
//    protected abstract void readFromDisk() throws Exception;
//    public abstract int newBar();
//  int key = msg.getData1();
//  int octave = (key / 12)-1;
//  int note = key % 12;
//  String noteName = NOTE_NAMES[note];
//  int velocity = msg.getData2();
//  System.out.println(cmd == NOTE_ON ? "NoteOn, " : "NoteOff, " + noteName + octave +
//  " key=" + key + " velocity: " + velocity);

//	public static void oomPah(MidiTrack track) throws InvalidMidiDataException {
//		Track t = track.getTrack();
//		track.getScheduler().init();
//		int res = track.getResolution();
//		clear(t);
//		int[] notes = new int[]{0, 36, 1, 40, 1, 43, 2, 31, 3, 40, 3, 43};
//
//		for (int i = 1; i < notes.length; i+=2) {
//			t.add(new MidiEvent(new ShortMessage(NOTE_ON, notes[i], 100), notes[i-1] * res ));
//			t.add(new MidiEvent(new ShortMessage(NOTE_OFF, notes[i], 1), (notes[i-1] + 1) * res ));
//		}
//		int offset = res * 4;
//		for (int i = 1; i < notes.length; i+=2) {
//			t.add(new MidiEvent(new ShortMessage(NOTE_ON, notes[i], 100), notes[i-1] * res + offset));
//			t.add(new MidiEvent(new ShortMessage(NOTE_OFF, notes[i], 1), (notes[i-1] + 1) * res + offset));
//		}
//		RTLogger.log(MidiTools.class, "oompah size " + t.size());
//	}
//public static AbstractCollection<MidiEvent> loadSection(long start, long end, Track t, AbstractCollection<MidiEvent> result) {
//	result.clear();
//	for (int i = 0; i < t.size(); i++) {
//		MidiEvent e = t.get(i);
//		if (e.getTick() < start) continue;
//		if (e.getTick() >= end) return result;
//		result.add(new MidiEvent(e.getMessage(), e.getTick() - start));
//	}
//	return result;
/// insert a clipboard midi event into the current two-bar window */
//public static MidiEvent interpolate(MidiEvent e, MidiTrack track) {
//	long base = e.getTick() >= track.getBarTicks() ? track.getRight() : track.getLeft();
//	long tick = e.getTick() + base;
//	return new MidiEvent(e.getMessage(), tick);
//}
//public static int measureCount(long ticks, int resolution, int beats) {
//return measureCount(ticks, measureTicks(resolution, beats));
//public static long measureTicks(int resolution, int beats) {
//return beats * resolution;
//public static void delete(int measure, MidiTrack track) {
//long start = measure * track.getBarTicks();
//long end = start + track.getBarTicks();
//long sub = track.getBarTicks();
//Track t = track.getT();
//int idx = fastFind(t, start);
//if (idx < 0)
//	return;
//for (int i = 0; i < t.size(); i++) {
//	MidiEvent e = t.get(i);
//	if (e.getTick() >= end)
//		e.setTick(e.getTick() - sub);
//	t.remove(e);
//}


