package net.judah.gui.widgets;

import java.util.ArrayList;

import net.judah.gui.Gui;
import net.judah.gui.Size;
import net.judah.seq.track.MidiTrack;

public class TrackAmp extends Slider {
	
	private static final ArrayList<TrackAmp> instances = new ArrayList<>();
	private final MidiTrack track;
	
	public TrackAmp(MidiTrack t) {
		super(0, 100, null, "Data2");
		Gui.resize(this, Size.SMALLER_COMBO);
		// super(Pastels.EGGSHELL);
		this.track = t;
		setValue((int) (track.getAmp() * 100));
		addChangeListener(evt->{
			if (track.getAmp() * 100 != getValue()) {
				track.setAmp(getValue() * 0.01f);
			}});
		instances.add(this);
	}
	
	public static void update(MidiTrack track) {
		instances.forEach(vol->{
			if (track == vol.track && vol.getValue() != track.getAmp() * 100)
				vol.setValue((int) (track.getAmp() * 100));
		});
	}

}
