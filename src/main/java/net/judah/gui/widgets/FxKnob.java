package net.judah.gui.widgets;

import java.awt.Color;

import javax.swing.JLabel;
import javax.swing.JPanel;

import net.judah.fx.Effect;
import net.judah.fx.EffectColor;
import net.judah.fx.Reverb;
import net.judah.gui.Gui;
import net.judah.gui.MainFrame;
import net.judah.gui.Pastels;
import net.judah.mixer.Channel;

/** a knob and label, paints itself on update when effect is active */
public class FxKnob extends JPanel {

	private final Effect effect;
	private final Channel ch;
	private final int idx;
	private final Knob knob;
	private final JLabel label;

	public FxKnob(Channel ch, Effect fx, int idx, String lbl) {
		effect = fx;
		this.ch = ch;
		this.idx = idx;
		knob = new Knob(value -> {
				fx.set(idx, value);
				MainFrame.update(ch);
			});
		knob.setKnobColor(Pastels.BLUE);
		add(knob);

		label = new JLabel(lbl, JLabel.LEFT);
		label.setFont(Gui.FONT11);
		add(label);
	}
	
	// inelegant realtime reverb
	public FxKnob(Channel ch, int idx, String lbl) {
		effect = null;
		this.ch = ch;
		this.idx = idx;
		knob = new Knob(value -> {
				ch.getReverb().set(idx, value);
				MainFrame.update(ch);
			});
		knob.setKnobColor(Pastels.BLUE);
		add(knob);

		label = new JLabel(lbl, JLabel.LEFT);
		label.setFont(Gui.FONT11);
		add(label);
	}
	
	public void update() {
		if (effect == null) {
			if (knob.getValue() != ch.getReverb().get(idx))
				knob.setValue(ch.getReverb().get(idx));
		}
		else if (knob.getValue() != effect.get(idx))
			knob.setValue(effect.get(idx));
		Color bg = effect == null ? ch.getReverb().isActive() ? EffectColor.get(Reverb.class) : null :
				effect.isActive() ? EffectColor.get(effect.getClass()) : null;
		setBackground(bg);
		label.setBackground(bg);
		knob.setBackground(bg);
	}
	
}
