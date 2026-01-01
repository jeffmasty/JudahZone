package net.judah.gui.widgets;

import java.awt.Dimension;

import javax.swing.JComboBox;

import judahzone.gui.Gui;
import net.judah.gui.Size;
import net.judah.seq.track.Cue;
import net.judah.seq.track.MidiTrack;

public class CueCombo extends JComboBox<Cue> {
	private static final Dimension DEFAULT_SIZE = new Dimension(60, Size.STD_HEIGHT);
	private final MidiTrack track;

	public CueCombo(MidiTrack t) {
		super(Cue.values());
		Gui.resize(this, DEFAULT_SIZE);
		setSelectedItem(t.getCue());
		this.track = t;
		addActionListener(e->action());
	}

	protected void action() {
		if (track.getCue() != getSelectedItem()) track.setCue((Cue)getSelectedItem());
	}

	public void update() {
		if (track.getCue() != getSelectedItem())
			setSelectedItem(track.getCue());
	}
//	public static void refresh(MidiTrack t) {
//		MainFrame.update(new TrackUpdate(Update.CUE, t));
//	}

}
