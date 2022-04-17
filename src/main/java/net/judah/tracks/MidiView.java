package net.judah.tracks;

import javax.swing.JLabel;

public class MidiView extends TrackView {

	public MidiView(MidiTrack track) {
		super(track);

		add(new JLabel("[channel]", JLabel.CENTER));
		add(new JLabel("[cycle]", JLabel.CENTER));
		add(new JLabel("[latch]", JLabel.CENTER));
		add(new JLabel("[arp]", JLabel.CENTER));
	
	}

}
