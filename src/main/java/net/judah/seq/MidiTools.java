package net.judah.seq;

import java.awt.Color;

public class MidiTools {
	public static final int NOTE_ON = 0x90;
    public static final int NOTE_OFF = 0x80;
    public static final String[] NOTE_NAMES = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
    public static final int NAME = 73;

    public static Color velocityColor(int data2) {
    	return new Color(0, 112, 60, data2 * 2); // Dartmouth Green
	}
	

    
//	/** move trailing midi forward */
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
	
	public static long measureTicks(int resolution, int beats) {
		return beats * resolution;
	}
	
	public static int measureCount(long ticks, long measureTicks) {
		return (int)Math.ceil(ticks / measureTicks);
	}
	
	public static int measureCount(long ticks, int resolution, int beats) {
		return measureCount(ticks, measureTicks(resolution, beats));
	
	}
	
	public static String formatTime(long timecode) {
		long millis = (long) (timecode * 1000.0 + 0.5);
		return String.format("%02d:%02d:%02d.%03d", millis / 60 / 60 / 1000, 
				(millis / 60 / 1000) % 60, (millis / 1000) % 60, millis % 1000);
	}

}
