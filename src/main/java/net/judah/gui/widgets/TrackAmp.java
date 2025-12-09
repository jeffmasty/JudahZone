package net.judah.gui.widgets;

import net.judah.seq.track.MidiTrack;

public class TrackAmp extends Slider {

	private final MidiTrack track;

	public TrackAmp(MidiTrack t) {
		super(0, 100, null, "Data2");

		this.track = t;
		setValue((int) (track.getAmp() * 100));
		addChangeListener(evt->{
			if (getValue() * 0.01f != track.getAmp()) {
				track.setAmp(getValue() * 0.01f);
			}});
	}

	public void update() {
		if (getValue() != track.getAmp() * 100)
			setValue((int) (track.getAmp() * 100));
	}

}
