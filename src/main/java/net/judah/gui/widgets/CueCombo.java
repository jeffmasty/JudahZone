package net.judah.gui.widgets;

import java.awt.Dimension;
import java.util.ArrayList;

import javax.swing.JComboBox;

import net.judah.gui.Gui;
import net.judah.gui.Size;
import net.judah.seq.track.Cue;
import net.judah.seq.track.MidiTrack;
import net.judah.util.Constants;

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

	// TODO hookup
	public static void refresh(MidiTrack t) {
		Constants.execute(()->{
			Cue cue = t.getCue();
			for (CueCombo c : instances)
				if (c.track == t && cue != c.getSelectedItem())
					c.setSelectedItem(t.getCue());
			});
	}
	
}
