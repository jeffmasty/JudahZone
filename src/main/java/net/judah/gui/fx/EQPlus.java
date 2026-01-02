package net.judah.gui.fx;

import java.awt.Color;

import javax.swing.JLabel;
import javax.swing.JPanel;

import judahzone.fx.EQ;
import judahzone.fx.EQ.EqBand;
import judahzone.gui.Gui;
import judahzone.gui.Pastels;
import judahzone.gui.Updateable;
import lombok.Getter;
import net.judah.channel.Channel;
import net.judah.gui.Bindings;
import net.judah.gui.widgets.Click;
import net.judah.gui.widgets.FxKnob;
import net.judah.gui.widgets.Knob;
import net.judah.gui.widgets.Knob.KnobListener;
import net.judah.seq.MidiConstants;

public class EQPlus implements Updateable {
	private static final Color SHIFTED = Pastels.MY_GRAY;

	@Getter private final EQKnob left, center, right;
	@Getter private final Click toggle = new Click("EQ+");
	private final Channel channel;
	private final EQ eq;

	public EQPlus(Channel ch) {
		this.channel = ch;
		this.eq = channel.getEq();
		left = new EQKnob(EqBand.Bass, eq);
		center = new EQKnob(EqBand.Mid, eq);
		right = new EQKnob(EqBand.High, eq);
		toggle.addActionListener(e->toggle());
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
			channel.setActive(eq, val > MidiConstants.THOLD_LO);
	}

	@Override public void update() {
		left.update();
		center.update();
		right.update();
	}

	class EQKnob extends JPanel implements Updateable, FXAware {

		private final EQ.EqBand band;
		@Getter private final EQ fx;
		private final Overloaded knob;
		private JLabel label;
		EQKnob(EQ.EqBand band, EQ fx) {
			this.band = band;
			this.fx = fx;
			knob = new Overloaded();
			add(knob);
			label = new JLabel(band.name(), JLabel.LEFT);
			label.setFont(Gui.FONT11);
			add(label);
		}


		@Override public void update() {
			knob.update();
			Color bg = channel.isActive(eq) ? Bindings.getFx(eq.getClass()) : null;
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
