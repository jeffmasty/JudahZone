package net.judah.gui.widgets;

import java.util.ArrayList;

import javax.swing.JComboBox;

import net.judah.omni.Threads;
import net.judah.seq.track.Gate;
import net.judah.seq.track.MidiTrack;
import net.judah.seq.track.TrackMenu;

public class GateCombo extends JComboBox<Gate> {

	private static ArrayList<GateCombo> instances = new ArrayList<>();
	private final MidiTrack track;

	public GateCombo(MidiTrack t) {
		super(Gate.values());
		this.track = t;
		setSelectedItem(t.getGate());
		addActionListener(e->t.setGate((Gate)getSelectedItem()));
		instances.add(this);
	}

	public static void refresh(MidiTrack t) {
		Threads.execute(()-> {
			TrackMenu.updateGate(t);
			for(GateCombo c : instances)
				if (c.track == t && c.getSelectedItem() != c.track.getGate())
					c.setSelectedItem(c.track.getGate());
		});
	}

}
