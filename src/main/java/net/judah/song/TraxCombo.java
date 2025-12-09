package net.judah.song;

import java.awt.event.ActionListener;

import javax.swing.JComboBox;

import net.judah.JudahZone;
import net.judah.gui.Gui;
import net.judah.seq.TrackList;
import net.judah.seq.automation.Automation;
import net.judah.seq.track.MidiTrack;

public class TraxCombo extends JComboBox<MidiTrack> {

	private ActionListener listener = (l) -> {
		MidiTrack t = (MidiTrack)getSelectedItem();
		if (t != null && t != Automation.getInstance().getTrack())
			Automation.getInstance().setTrack(t);
	};

	TraxCombo() {
		setFont(Gui.BOLD);
		addActionListener(listener);
	}

	public void update() {
		if (Automation.getInstance().getTrack() != getSelectedItem())
			setSelectedItem(Automation.getInstance().getTrack());
	}

	void refill(TrackList<MidiTrack> trackList) {
		removeActionListener(listener);
		removeAllItems();
		addItem(JudahZone.getSeq().getMains());
		for (MidiTrack t : trackList)
			addItem(t);
		update();
		addActionListener(listener);
	}


}
