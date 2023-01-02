package net.judah.widgets;

import java.awt.Color;

import javax.swing.JLabel;
import javax.swing.JPanel;

import net.judah.effects.EffectColor;
import net.judah.effects.LFO;
import net.judah.effects.api.Effect;
import net.judah.gui.Gui;
import net.judah.gui.MainFrame;
import net.judah.gui.Pastels;
import net.judah.mixer.Channel;

/** a knob and label, paints itself on update when effect is active */
public class JudahKnob extends JPanel {

	private final Effect effect;
	private final int idx;
	
	private final Knob knob;
	private final JLabel label;

	public JudahKnob(Channel ch, Effect fx, int idx, String lbl) {
		effect = fx;
		this.idx = idx;
		knob = new Knob(value -> {
				fx.set(idx, value);
				MainFrame.update(ch);
			});
		knob.setKnobColor(fx instanceof LFO ? Pastels.PURPLE : Pastels.BLUE);
		add(knob);

		label = new JLabel(lbl, JLabel.LEFT);
		label.setFont(Gui.FONT11);
		add(label);
	}
	
	public void update() {
		if (knob.getValue() != effect.get(idx))
			knob.setValue(effect.get(idx));
		Color bg = effect.isActive() ? EffectColor.get(effect.getClass()) : null;
		setBackground(bg);
		label.setBackground(bg);
		knob.setBackground(bg);
	}
	
}
