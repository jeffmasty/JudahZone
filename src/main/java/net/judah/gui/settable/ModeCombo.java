package net.judah.gui.settable;

import java.util.ArrayList;

import net.judah.gui.Gui;
import net.judah.gui.Size;
import net.judah.seq.arp.Arp;
import net.judah.seq.track.PianoTrack;

public class ModeCombo extends SetCombo<Arp> {

	private final PianoTrack track;
	private static final ArrayList<ModeCombo> instances = new ArrayList<>();

	public ModeCombo(PianoTrack t) {
		super(Arp.values(), t.getArp());
		this.track = t;
		instances.add(this);
		Gui.resize(this, Size.MODE_SIZE);
		setOpaque(true);
	}

	@Override
	protected void action() {
		track.setArp((Arp)getSelectedItem());
		setBackground(track.getArp().getColor());
	}

	public static void update(PianoTrack t) {
		for (ModeCombo c : instances) {
			if (c.track != t) continue;

			if (c.getSelectedItem() == c.track.getArp())
				continue;
			c.override(t.getArp());
		}
	}

	@Override
	public void override(Arp val) {
		super.override(val);
		setBackground(track.getArp().getColor());
	}


}
