package net.judah.gui.fx;

import java.awt.Color;

import javax.swing.JLabel;
import javax.swing.JPanel;

import lombok.Getter;
import net.judah.fx.EQ;
import net.judah.fx.EQ.EqBand;
import net.judah.fx.EffectColor;
import net.judah.gui.Gui;
import net.judah.gui.Pastels;
import net.judah.gui.Updateable;
import net.judah.gui.widgets.Click;
import net.judah.gui.widgets.FxKnob;
import net.judah.gui.widgets.Knob;
import net.judah.gui.widgets.Knob.KnobListener;
import net.judah.mixer.Channel;
import net.judah.seq.MidiConstants;

public class EQPlus implements Updateable {
	private static final Color SHIFTED = Pastels.MY_GRAY;

	@Getter private final EQKnob left = new EQKnob(EqBand.Bass);
	@Getter private final EQKnob center = new EQKnob(EqBand.Mid);
	@Getter private final EQKnob right = new EQKnob(EqBand.High);
	@Getter private final Click toggle = new Click("EQ+");
	private final Channel channel;
	private final EQ eq;

	public EQPlus(Channel ch) {
		this.channel = ch;
		this.eq = channel.getEq();
		toggle.addActionListener(l->toggle());
	}

	public boolean isShift() { return toggle.getBackground() == SHIFTED; }
	public void toggle() {
		toggle.setBackground(isShift() ? null : SHIFTED);
		update();
	}

	public void knob(EqBand band, boolean up) {
		int idx = band.ordinal() + (isShift() ? 3 : 0);
		int val = EffectsRack.offset(eq.get(idx), up);
		eq.set(idx, val);
		if (idx < 3)
			eq.setActive(val > MidiConstants.THOLD_LO);
	}

	@Override public void update() {
		left.update();
		center.update();
		right.update();
	}

	class EQKnob extends JPanel implements Updateable {
		private EQ.EqBand band;
		private Overloaded knob;
		private JLabel label;
		EQKnob(EQ.EqBand band) {
			this.band = band;
			knob = new Overloaded();
			add(knob);
			label = new JLabel(band.name(), JLabel.LEFT);
			label.setFont(Gui.FONT11);
			add(label);
		}
		@Override public void update() {
			knob.update();
			Color bg = eq.isActive() ? EffectColor.get(eq.getClass()) : null;
			setBackground(bg);
			label.setBackground(bg);
			knob.setBackground(bg);

			int offset = isShift() ? 3 : 0;
			int idx = band.ordinal() + offset;
			String target = EQ.Settings.values()[idx].name();
			if (label.getText() != target)
				label.setText(target);
		}

		class Overloaded extends Knob implements KnobListener, Updateable {
			Overloaded() {
				setKnobColor(FxKnob.THUMB);
				addListener(this);
			}

			@Override public void update() {
				int offset = isShift() ? 3 : 0;
				int idx = band.ordinal() + offset;
				if (getValue() != eq.get(idx));
				setValue(eq.get(idx));
			}

			@Override public void knobChanged(int val) {
				int shift = isShift() ? 3 : 0;
				eq.set(band.ordinal() + shift, val);
			}

		} // end Overloaded

	} // end knob panel


}
