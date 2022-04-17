package net.judah.tracks;

import javax.swing.JComboBox;
import javax.swing.JLabel;

public class KitView extends TrackView {

	public KitView(Track track) { // TODO
		super(track);

		add(new JLabel("[pattern]", JLabel.CENTER));
		
		JComboBox<Character> patterns = new JComboBox<Character>();
		
		add(new JLabel("[cycle]", JLabel.CENTER));
		add(new JLabel("[latch]", JLabel.CENTER));
		add(new JLabel("[arp]", JLabel.CENTER));
	}

	
	
}
