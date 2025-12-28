package net.judah.song;

import java.awt.event.ActionListener;

import javax.swing.JComboBox;

import net.judah.gui.Gui;
import net.judah.seq.Seq;
import net.judah.seq.TrackList;
import net.judah.seq.track.MidiTrack;

public class TraxCombo extends JComboBox<MidiTrack> {

	private final Seq seq;
	private final ActionListener listener;

	public TraxCombo(Seq seq) {
		this.seq = seq;
		setFont(Gui.BOLD);
		listener = (l)-> {
			MidiTrack t = (MidiTrack)getSelectedItem();
			if (t != null && t != seq.getAutomation().getTrack())
				seq.getAutomation().setTrack(t);};
		addActionListener(listener);
	}

	public void update(MidiTrack t) {
		if (t != getSelectedItem())
			setSelectedItem(t);
	}

	public void refill(TrackList<MidiTrack> trackList, MidiTrack selected) {
		removeActionListener(listener);
		removeAllItems();
		addItem(seq.getMains());
		for (MidiTrack t : trackList)
			addItem(t);
		update(selected);
		addActionListener(listener);
	}


}
