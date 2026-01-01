package net.judah.gui.settable;

import judahzone.gui.Gui;
import net.judah.gui.Size;
import net.judah.seq.arp.Arp;
import net.judah.seq.track.PianoTrack;

public class ModeCombo extends SetCombo<Arp> {

	private final PianoTrack track;

	public ModeCombo(PianoTrack t) {
		super(Arp.values(), t.getArp());
		this.track = t;
		Gui.resize(this, Size.MODE_SIZE);
		setOpaque(true);
	}

	@Override
	protected void action() {
		track.setArp((Arp)getSelectedItem());
		setBackground(track.getArp().getColor());
	}

	@Override
	public void override(Arp val) {
		super.override(val);
		setBackground(track.getArp().getColor());
	}

	public void update() {
		if (getSelectedItem() == track.getArp())
			return;
		override(track.getArp());
	}

}
