package net.judah.sequencer;

import java.util.ArrayList;

import net.judah.util.Console;

public class MidiTracks extends ArrayList<MidiTrack> {

	private int transpose = 0;
	
	float tempo;
	
	public void setTranspose(int steps) {
		if (transpose != steps) {
			Console.info("midi transpose: " + steps);
			transpose = steps;
			forEach(track -> { track.setTranspose(steps);});
		}
	}

	public void setGain(int idx, float gain) {
		get(idx).setGain(gain);
	}
	
}
