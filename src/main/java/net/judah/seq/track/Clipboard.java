package net.judah.seq.track;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.sound.midi.MidiEvent;

import lombok.Getter;

public class Clipboard {

	private final ArrayDeque<MidiEvent> list = new ArrayDeque<>();
	/** source track resolution */
	@Getter private int resolution;

//	public void copy(Notes selected, MidiTrack track) {
//		list.clear();
//		resolution = track.getResolution();
//		long left = track.getLeft();
//		for (MidiNote p : selected) {
//			p = MidiTools.zeroBase(p, left);
//			list.add(p);
//		}
//	}

	public void copy(Collection<MidiEvent> selected, MidiTrack track) {
		list.clear();
		resolution = track.getResolution();
		long left = track.getLeft();
		selected.forEach(e -> list.add(MidiTools.zeroBase(left, e)));
	}

	public List<MidiEvent> paste(MidiTrack track) {
		ArrayList<MidiEvent> result = new ArrayList<>();
		float ratio = track.getResolution() / (float)resolution;
		long offset = track.getLeft();
		for (MidiEvent e : list)
			result.add(new MidiEvent(e.getMessage(), (long)(e.getTick() * ratio) + offset));
		return result;
	}

//	public List<MidiPair> paste(MidiTrack track) {
//		ArrayList<MidiPair> result = new ArrayList<>();
//		long offset = track.getLeft();
//		float ratio = track.getResolution() / (float)resolution;
//		for (MidiPair p : list) {
//			MidiEvent off = null;
//			if (p.getOff() != null)
//				off = new MidiEvent(p.getOff().getMessage(), (long)(p.getOff().getTick() * ratio) + offset);
//			result.add(new MidiPair(new MidiEvent(p.getOn().getMessage(), (long)(p.getOn().getTick() * ratio) + offset), off));
//		}
//		return result;
//	}


}
