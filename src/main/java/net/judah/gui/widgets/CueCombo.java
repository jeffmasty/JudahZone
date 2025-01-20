package net.judah.gui.widgets;

import java.awt.Dimension;
import java.util.ArrayList;

import javax.swing.JComboBox;

import net.judah.gui.Gui;
import net.judah.gui.Size;
import net.judah.omni.Threads;
import net.judah.seq.track.TrackMenu;
import net.judah.seq.track.Cue;
import net.judah.seq.track.MidiTrack;

public class CueCombo extends JComboBox<Cue> {
	private static final Dimension DEFAULT_SIZE = new Dimension(60, Size.STD_HEIGHT);
	private static final ArrayList<CueCombo> instances = new ArrayList<>();
	private final MidiTrack track;

	public CueCombo(MidiTrack t) {
		super(Cue.values());
		Gui.resize(this, DEFAULT_SIZE);
		setSelectedItem(t.getCue());
		this.track = t;
		instances.add(this);
		addActionListener(e->action());
	}

	protected void action() {
		if (track.getCue() != getSelectedItem()) track.setCue((Cue)getSelectedItem());
	}

	public static void refresh(MidiTrack t) {
		Threads.execute(()->{
			TrackMenu.updateCue(t);
			Cue cue = t.getCue();
			for (CueCombo c : instances) {
				if (c.track != t)
					continue;
				if (cue != c.getSelectedItem())
					c.setSelectedItem(t.getCue());
			}
			});
	}

}
