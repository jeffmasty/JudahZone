package net.judah.gui.widgets;

import java.util.ArrayList;

import net.judah.gui.Pastels;
import net.judah.omni.Threads;
import net.judah.seq.track.MidiTrack;

public class TrackVol extends Knob {

	private static final ArrayList<TrackVol> instances = new ArrayList<>();
	private final MidiTrack track;

	public TrackVol(MidiTrack t) {
		super(Pastels.EGGSHELL);
		this.track = t;
		setValue((int) (track.getAmp() * 100));
		addListener(val->{
			if (track.getAmp() * 100 != val) {
				track.setAmp(val * 0.01f);
			}});
		instances.add(this);
	}

	public static void update(MidiTrack track) {
		Threads.execute(() ->
			instances.forEach(vol->{
				if (track == vol.track && vol.getValue() != track.getAmp() * 100)
					vol.setValue((int) (track.getAmp() * 100));
		}));
	}

}
