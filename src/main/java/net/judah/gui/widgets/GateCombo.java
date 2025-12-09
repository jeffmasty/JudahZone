package net.judah.gui.widgets;

import javax.swing.JComboBox;

import net.judah.seq.track.Gate;
import net.judah.seq.track.NoteTrack;

public class GateCombo extends JComboBox<Gate> {

	private final NoteTrack track;

	public GateCombo(NoteTrack t) {
		super(Gate.values());
		this.track = t;
		setSelectedItem(t.getGate());
		addActionListener(e->t.setGate((Gate)getSelectedItem()));
	}

	public void update() {
		if (getSelectedItem() != track.getGate())
			setSelectedItem(track.getGate());
	}


}
