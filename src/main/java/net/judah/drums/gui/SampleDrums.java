package net.judah.drums.gui;

import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.function.Supplier;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import judahzone.gui.Updateable;
import lombok.Getter;
import net.judah.drums.DrumType;
import net.judah.drums.gui.SampleDrums.Modes;
import net.judah.drums.oldschool.OldSchool;
import net.judah.drums.oldschool.SampleParams;
import net.judah.midi.Actives;

public class SampleDrums extends DrumKnobs implements Updateable, Supplier<Modes> {

	public static enum Modes {
		Vol, Pan, Attack, Decay; // Dist, Filter
	}

	@Getter private final OldSchool kit;
	private final ArrayList<KitPad> pads = new ArrayList<>(DrumType.values().length);
	private volatile Modes mode;

	private class ModeButton extends JToggleButton {
		private final Modes m;
		public ModeButton(Modes mode) {
			super(mode.name());
			this.m = mode;
			addActionListener(e -> {
				SampleDrums.this.mode = m;
				pads.forEach(p -> p.updateMode());
			});
		}
	}

	public SampleDrums(OldSchool drumz) {
		kit = drumz;
		JPanel modes = new JPanel();
		ButtonGroup btns = new ButtonGroup();
		for (Modes m : Modes.values()) {
			ModeButton b = new ModeButton(m);
			btns.add(b);
			modes.add(b);
		}
		// select first button and initialize local mode so callers won't see null
		if (btns.getElements().hasMoreElements()) {
			btns.getElements().nextElement().setSelected(true);
			mode = Modes.values()[0];
		}
		JPanel wrap = new JPanel(new GridLayout(2, 4, 1, 1));

		for (DrumType t : DrumType.values()) {
			KitPad pad = new KitPad(drumz, drumz.getSample(t), this);
			pads.add(pad);
			wrap.add(pad);
		}

		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		add(Box.createVerticalStrut(1));
		add(wrap);
		add(Box.createVerticalStrut(1));
		add(modes);
		add(Box.createVerticalStrut(1));
		update();
	}

	@Override
	public Modes get() {
		return mode;
	}

	public void update(SampleParams p) {
		for (KitPad pad : pads)
			if (pad.getDrum() == p.drum())
				pad.update();
	}

	@Override public void update() {
		pads.forEach(p -> p.update());
	}

	@Override
	public void doKnob(int idx, int data2) {
		pads.get(idx).knobChanged(data2);
	}

//	@Override public void pad1() {
//		Threads.execute(() -> {
//		int idx = trax.getSelectedIndex() + 1;
//		if (idx >= trax.getItemCount())
//			idx = 0;
//		trax.setSelectedItem(idx); });
//	}

	/** Cycle to next param in active tab (pad2). */
	public void pad2() {
		int modeIdx = (mode.ordinal() + 1) % Modes.values().length;
		mode = Modes.values()[modeIdx];
		pads.forEach(p -> p.updateMode());
	}


	public void update(Actives a) {
		pads.forEach(pad -> pad.background(a));
	}

	@Override
	public void update(Object o) {
		if (o instanceof Actives a && a == kit.getActives())
			update(a);
		else if (o instanceof SampleParams sp)
			update(sp);
	}

}
