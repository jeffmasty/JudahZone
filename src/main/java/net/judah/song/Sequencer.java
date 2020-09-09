package net.judah.song;

import javax.swing.JButton;
import javax.swing.JPanel;

public class Sequencer extends JPanel implements Edits {

	public Sequencer() {
		add(new JButton("Push Me!"));
	}

	@Override public void add() {
	}

	@Override public void delete() {
	}

	@Override public void copy() {
	}
	
}
