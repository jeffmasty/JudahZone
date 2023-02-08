package net.judah.gui.widgets;

import java.util.ArrayList;

import net.judah.gui.Pastels;
import net.judah.seq.MidiTrack;
import net.judah.util.Constants;

public class TrackVol extends Knob {
	
	private static final ArrayList<TrackVol> instances = new ArrayList<>();
	private final MidiTrack track;
	
	public TrackVol(MidiTrack t) {
		super(Pastels.EGGSHELL);
		this.track = t;
		setValue((int) (track.getAmp() * 100));
		addListener(val->{
			if (track.getAmp() * 0.01f != val) {
				track.setAmp(val * 0.01f);
			}});
		instances.add(this);
	}
	
	public static void update(MidiTrack t) {
		Constants.execute(()->{
			instances.forEach(vol->{
				if (t == vol.track && vol.getValue() != t.getAmp() * 100)
					vol.setValue((int) (t.getAmp() * 100));
			});
		});
	}

}
