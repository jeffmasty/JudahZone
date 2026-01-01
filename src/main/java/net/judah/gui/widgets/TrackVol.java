package net.judah.gui.widgets;

import judahzone.gui.Pastels;
import net.judah.seq.track.MidiTrack;

public class TrackVol extends Knob {

	private final MidiTrack track;

	public TrackVol(MidiTrack t) {
		super(Pastels.EGGSHELL);
		this.track = t;
		setValue((int) (track.getAmp() * 100));
		addListener(val->{
			if (track.getAmp() * 100 != val) {
				track.setAmp(val * 0.01f);
			}});
	}

	public void update() {
		if (getValue() != track.getAmp() * 100)
			setValue((int) (track.getAmp() * 100));
	}

}
