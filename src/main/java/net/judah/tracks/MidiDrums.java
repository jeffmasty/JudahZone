package net.judah.tracks;

import javax.swing.JLabel;

public class MidiDrums extends TrackView {

	public MidiDrums(MidiTrack track) { // TODO a second midi track?
		super(track);

		add(new JLabel("[File]", JLabel.CENTER));
		add(new JLabel("[midiOut]", JLabel.CENTER));
		add(new JLabel("[inst1]", JLabel.CENTER));
		add(new JLabel("[vol]", JLabel.CENTER));
	
	}

}
