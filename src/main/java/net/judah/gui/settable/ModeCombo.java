package net.judah.gui.settable;

import java.util.ArrayList;

import net.judah.seq.MidiTrack;
import net.judah.seq.Mode;

public class ModeCombo extends SetCombo<Mode> {
	
	private final MidiTrack track;
	private static final ArrayList<ModeCombo> instances = new ArrayList<>();
	
	public ModeCombo(MidiTrack t) {
		super(Mode.values(), t.getArp().getMode());
		this.track = t;
	}

	@Override
	protected void action() {
		track.getArp().setMode((Mode)getSelectedItem());
		setBackground(track.getArp().getMode().getColor());
	}
	
	public static void update(MidiTrack t) {
		for (ModeCombo c : instances) {
			if (c.track != t) continue;
			if (c.getSelectedItem() == c.track.getArp().getMode())
				return;
			c.override(t.getArp().getMode());
			return;
		}
	}



}
