package net.judah.seq;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

import javax.sound.midi.MidiEvent;

import lombok.Getter;
import net.judah.seq.track.MidiTrack;

public class Clipboard {

	private final ArrayDeque<MidiPair> list = new ArrayDeque<>();
	/** source track resolution */
	@Getter private int resolution;

	public void copy(Notes selected, MidiTrack track) {
		list.clear();
		resolution = track.getResolution();
		for (MidiPair p : selected)
			list.add(MidiTools.zeroBase(p, track.getLeft()));
	}

	public List<MidiPair> paste(MidiTrack track) {
		ArrayList<MidiPair> result = new ArrayList<>();
		long offset = track.getLeft();
		float ratio = track.getResolution() / (float)resolution;
		for (MidiPair p : list) {
			MidiEvent off = null;
			if (p.getOff() != null)
				off = new MidiEvent(p.getOff().getMessage(), (long)(p.getOff().getTick() * ratio) + offset);
			result.add(new MidiPair(new MidiEvent(p.getOn().getMessage(), (long)(p.getOn().getTick() * ratio) + offset), off));
		}
		return result;
	}


}
