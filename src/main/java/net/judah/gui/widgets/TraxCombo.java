package net.judah.gui.widgets;

import javax.swing.JComboBox;

import net.judah.JudahZone;
import net.judah.gui.Gui;
import net.judah.seq.automation.Automation;
import net.judah.seq.track.MidiTrack;

public class TraxCombo extends JComboBox<MidiTrack> {

	Automation view;

	public TraxCombo(Automation view) {
		super(JudahZone.getSeq().getTracks());
		setFont(Gui.BOLD);
		this.view = view;
		setSelectedItem(JudahZone.getSeq().getCurrent());
		addActionListener(l->change());
	}

	private void change() {
		MidiTrack t = (MidiTrack)getSelectedItem();
		if (t != view.getTrack())
			view.setTrack(t);;
	}
}
