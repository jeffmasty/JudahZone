package net.judah.seq;

import java.awt.Color;
import java.util.AbstractCollection;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

import net.judah.JudahZone;

public class MidiTools {
	public static final int NOTE_ON = 0x90;
    public static final int NOTE_OFF = 0x80;
    public static final String[] NOTE_NAMES = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
	public static final int RATCHET = 11;

    private static final Pattern work = new Pattern();

//	public static void paste(Bar source, int measure) {
//	}
//	public static void copy(long start, long end, long newPosition, Track t) {
//	}

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
	
	public static boolean match(MidiPair it, MidiPair other) {
		return match(it.getOn(), other.getOn()) && match(it.getOff(), other.getOff());
	}
	public static boolean match(MidiEvent it, MidiEvent other) {
		if (it == null && other == null) return true;
		if (it == null || other == null) return false;
		if (it.getTick() != other.getTick()) return false;
		return match(it.getMessage(), other.getMessage());
	}
	
    public static Color velocityColor(int data2) {
    	return new Color(0, 112, 60, data2 * 2); // Dartmouth Green
	}
    
    public static Color highlightColor(int data2) {
    	return new Color(0xFF, 0xA5, 0x00, data2 * 2); // Orange
    }
	
	public static long measureTicks(int resolution, int beats) {
		return beats * resolution;
	}
	
	public static int measureCount(long ticks, long measureTicks) {
		return (int)Math.ceil(ticks / measureTicks) + 1;
	}
	
	public static int measureCount(long ticks, int resolution, int beats) {
		return measureCount(ticks, measureTicks(resolution, beats));
	}
	
	/** loads one bar from measure of track into the supplied result, zero-basing the ticks*/
	public static AbstractCollection<MidiEvent> loadMeasure(int measure, MidiTrack track, AbstractCollection<MidiEvent> result) {
		long start = measure * track.getBarTicks();
		long end = start + track.getBarTicks();
		return loadSection(start, end, track.getT(), result);
	}
	
	public static AbstractCollection<MidiEvent> loadSection(long start, long end, Track t, AbstractCollection<MidiEvent> result) {
		result.clear();
		for (int i = 0; i < t.size(); i++) {
			MidiEvent e = t.get(i);
			if (e.getTick() < start) continue;
			if (e.getTick() >= end) return result;
			result.add(new MidiEvent(e.getMessage(), e.getTick() - start));
		}
		return result;
	}

	/** copy/paste given measure to end of track */
	public static void append(int measure, MidiTrack track) {
		int target = measureCount(track.getT().ticks(), track.getBarTicks()) + 1;
		long base = target * track.getBarTicks();
		Track t = track.getT();
		loadMeasure(measure, track, work);
		for (MidiEvent e : work) {
			t.add(new MidiEvent(e.getMessage(), e.getTick() + base));
		}
		
	}
	
	/** @return true if delete was matched and removed from track */
	public static boolean delete(MidiEvent delete, Track t) {
		long tick = delete.getTick();
		for (int i = 0; i < t.size(); i++) {
			MidiEvent e = t.get(i);
			if (e.getTick() < tick) continue;
			if (e.getTick() > tick) return false;
			if (match(e.getMessage(), delete.getMessage())) {
					t.remove(e);
					return true;
			}
		}
		return false;
	}
	
	public static void delete(int measure, MidiTrack track) {
		long start = measure * track.getBarTicks();
		long end = start + track.getBarTicks();
		long sub = track.getBarTicks();
		Track t = track.getT();
		for (int i = 0; i < t.size(); i++) {
			MidiEvent e = t.get(i);
			if (e.getTick() < start) continue;
			if (e.getTick() >= end)
				e.setTick(e.getTick() - sub);
			t.remove(e);
		}
	}
	
	/** insert a clipboard midi event into the current two-bar window */
	public static void interpolate(MidiEvent e, MidiTrack track) {
		long base = e.getTick() >= track.getBarTicks() ? track.getRight() : track.getLeft();
//		int bar = track.getCurrent();
//		if (e.getTick() >= track.getBarTicks())
//			bar = track.getNext();
		long tick = e.getTick() + base;// * track.getBarTicks();
		track.getT().add(new MidiEvent(e.getMessage(), tick));
	}
	
	
	public static String formatTime(long timecode) {
		long millis = (long) (timecode * 1000.0 + 0.5);
		return String.format("%02d:%02d:%02d.%03d", millis / 60 / 60 / 1000, 
				(millis / 60 / 1000) % 60, (millis / 1000) % 60, millis % 1000);
	}
	

	public static long quantizePlus(long tick, Gate type, int resolution) {
		switch(type) {
		case SIXTEENTH: return quantize(tick, type, resolution) + (resolution / 4);
		case EIGHTH:	return quantize(tick, type, resolution) + (resolution / 2);
		case QUARTER:	return quantize(tick, type, resolution) + (resolution);
		case HALF:		return quantize(tick, type, resolution) + (2 * resolution);
		case WHOLE: 	return JudahZone.getClock().getMeasure() * resolution;
		case MICRO:		return quantize(tick, type, resolution) + (resolution / 8);
		case RATCHET:	return quantize(tick, type, resolution) + RATCHET;
		default: return tick;
		}
	}

	public static long quantize(long tick, Gate type, int resolution) {
		switch(type) {
		case SIXTEENTH: return tick - tick % (resolution / 4);
		case EIGHTH: return tick - tick % (resolution / 2);
		case QUARTER: return tick - tick % resolution;
		case HALF: return tick - tick % (2 * resolution);
		case WHOLE: return 0;
		case MICRO: return tick - tick % (resolution / 8);
		case RATCHET: return tick - tick % RATCHET; // approx MIDI_24
		default: // NONE
			return tick;
		}
	}
	

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
//		for (MidiEvent e : b) {
//			result.add(new MidiPair(new MidiEvent(e.getMessage(), e.getTick() + (first ? 0 : barTicks)), null));
//		}
//	}

//	public static void publishBeats(MidiTrack track, Measure result) {
//		long barTicks = track.getBarTicks();
//		result.clear();
////		if (track.getView().isLive()) {
////			// setStartRef
////		}
////		else {
//			if (track.isEven()) { // even, display current | next
//				result.startTick = track.getCurrent() * barTicks;
//				result.setBTick(track.getNext() * barTicks);
//				for (MidiEvent e : loadMeasure(track.getCurrent(), track, work)) 
//					result.add(new MidiPair(e, null));
//				for (MidiEvent e : loadMeasure(track.getNext(), track, work)) 
//					result.add(new MidiPair(new MidiEvent(e.getMessage(), e.getTick() + barTicks), null));
//			}
//			else { // odd, display previous | current
//				result.startTick = track.getPrevious() * barTicks;
//				result.setBTick(track.getCurrent() * barTicks);
//				for (MidiEvent e : loadMeasure(track.getPrevious(), track, work)) 
//					result.add(new MidiPair(e, null));
//				for (MidiEvent e : loadMeasure(track.getCurrent(), track, work)) 
//					result.add(new MidiPair(new MidiEvent(e.getMessage(), e.getTick() + barTicks), null));
//			}
////		}
//	}
	
	
//	public static void publishPiano(MidiTrack track, Measure result) {
//		long barTicks = track.getBarTicks();
//		if (track.getView().isLive()) {
//			// setStartRef
//		}
//		else {
//		result.load(track);
//		result.load(track);
//			if (track.isEven()) { // even, display current | next
//				result.startTick = track.getCurrent() * barTicks;
//				result.setBTick(track.getNext() * barTicks);
//				for (MidiEvent e : loadMeasure(track.getCurrent(), track, work)) 
//					result.add(new MidiPair(e, null));
//				for (MidiEvent e : loadMeasure(track.getNext(), track, work)) 
//					result.add(new MidiPair(new MidiEvent(e.getMessage(), e.getTick() + barTicks), null));
//			}
//			else { // odd, display previous | current
//				result.startTick = track.getPrevious() * barTicks;
//				result.setBTick(track.getCurrent() * barTicks);
//				for (MidiEvent e : loadMeasure(track.getPrevious(), track, work)) 
//					result.add(new MidiPair(e, null));
//				for (MidiEvent e : loadMeasure(track.getCurrent(), track, work)) 
//					result.add(new MidiPair(new MidiEvent(e.getMessage(), e.getTick() + barTicks), null));
//			}
///////////////		
//			result.startTick = track.getCurrent() * barTicks;
//			loadMeasure(track.getCurrent(), track, work);
//			if (track.isDrums()) {
//				for (MidiEvent e : work) 
//					result.add(new MidiPair(e, null));
//			}
//			else 
//				; //collateNotes(work, track.getBarTicks(), result.a, result);
//			
//			result.setBTick(track.getNext() * track.getBarTicks());
//			loadMeasure(track.getNext(), track, work);
////			collateNotes(work, track.getBarTicks(), result.b, result);
//			
////		}
//		
		
//		snip(work, track.getBarTicks(), 0, result.a);
//
//		long start = result.getStartref() % barTicks; 
//		long end = barTicks - start;
//
//		MidiTools.loadMeasure(scheduler.current, this, result.one);
//		snip(result.one, start, barTicks, 0, result);
//		
//		MidiTools.loadMeasure(scheduler.next, this, result.two);
//		snip(result.two, 0, barTicks, barTicks - start, result);
//		if (end != barTicks) {
//			MidiTools.loadMeasure(scheduler.afterNext, this, result.three);
//			snip(result.three, 0, end, (2 * barTicks) - start, result);
//		}
//		else 
//			result.three = null;
	
		// anything left in accumulator?
//		for (MidiEvent e : result.stash) {
//			ShortMessage on = (ShortMessage)e.getMessage();
//			result.add(new Note(e.getTick(), 2 * barTicks, on.getData1(), on.getData2()));
//		}
	}
	
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
//			else if (e.getTick() < end) {
//				track.remove(e);
//			}
//			else {
//				cleansed = true;
//				e.setTick(e.getTick() - measure);
//			}
//		}
//	}

	
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

	
//	public static void copyMeasure(Track track, long start, long end, ArrayList<MidiEvent> result, long offset) {
//		for (int i = 0; i < track.size(); i++) {
//			MidiEvent e = track.get(i);
//			if (e.getMessage() instanceof ShortMessage == false)
//				continue;
//			long tick = e.getTick();
//			if (tick < start)
//				continue;
//			if (tick < end || (tick == end && e.getMessage().getStatus() == NOTE_OFF))
//				result.add(new MidiEvent(e.getMessage(), e.getTick() - start + offset));
//			else
//				return;
//		}
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
