package net.judah.gui.settable;

import java.awt.Dimension;
import java.util.ArrayList;

import net.judah.gui.Gui;
import net.judah.seq.MidiTrack;
import net.judah.seq.arp.Mode;
import net.judah.util.Constants;

public class ModeCombo extends SetCombo<Mode> {
	public static Dimension SIZE = new Dimension(70, 26);

	private final MidiTrack track;
	private static final ArrayList<ModeCombo> instances = new ArrayList<>();
	
	public ModeCombo(MidiTrack t) {
		super(Mode.values(), t.getArp().getMode());
		this.track = t;
		instances.add(this);
		Gui.resize(this, SIZE);
		setOpaque(true);
	}

	@Override
	protected void action() {
		track.getArp().setMode((Mode)getSelectedItem());
		setBackground(track.getArp().getMode().getColor());
	}
	
	public static void update(MidiTrack t) {
		Constants.execute(()->{
			for (ModeCombo c : instances) {
				if (c.track != t) continue;
				if (c.getSelectedItem() == c.track.getArp().getMode()) {
					c.repaint();
					continue;
				}
				c.override(t.getArp().getMode());
		}});
	}

	@Override
	public void override(Mode val) {
		super.override(val);
		setBackground(track.getArp().getMode().getColor());
	}


}
