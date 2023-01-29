package net.judah.gui.widgets;

import net.judah.gui.Pastels;
import net.judah.seq.MidiTrack;

public class TrackVol extends Knob {
	
	private final MidiTrack track;
	
	public TrackVol(MidiTrack t) {
		super(Pastels.EGGSHELL);
		this.track = t;
		setValue(track.getAmplification());
		addListener(val->{
			if (track.getAmplification() != val) {
				track.setAmplification(val);
		}});
	}

}
