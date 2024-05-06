package net.judah.gui.settable;

import java.util.ArrayList;

import net.judah.gui.Gui;
import net.judah.gui.Size;
import net.judah.seq.arp.Mode;
import net.judah.seq.track.PianoTrack;

public class ModeCombo extends SetCombo<Mode> {

	private final PianoTrack track;
	private static final ArrayList<ModeCombo> instances = new ArrayList<>();
	
	public ModeCombo(PianoTrack t) {
		super(Mode.values(), t.getArp().getMode());
		this.track = t;
		instances.add(this);
		Gui.resize(this, Size.MODE_SIZE);
		setOpaque(true);
	}

	@Override
	protected void action() {
		track.getArp().setMode((Mode)getSelectedItem());
		setBackground(track.getArp().getMode().getColor());
	}
	
	public static void update(PianoTrack t) {
		for (ModeCombo c : instances) {
			if (c.track != t) continue;
			
			if (c.getSelectedItem() == c.track.getArp().getMode()) 
				continue;
			c.override(t.getArp().getMode());
		}
	}

	@Override
	public void override(Mode val) {
		super.override(val);
		setBackground(track.getArp().getMode().getColor());
	}


}
