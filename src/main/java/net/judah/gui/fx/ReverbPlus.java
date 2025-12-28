package net.judah.gui.fx;

import static net.judah.fx.Reverb.Settings.*;

import java.awt.Color;

import javax.swing.JLabel;

import lombok.Getter;
import net.judah.fx.Reverb;
import net.judah.gui.Gui;
import net.judah.gui.HQ;
import net.judah.gui.Pastels;
import net.judah.gui.Updateable;
import net.judah.mixer.Channel;


public class ReverbPlus implements Updateable {

	private final Channel ch;
	private final Reverb reverb;
	final JLabel lLbl = new JLabel(Wet.name(), JLabel.LEFT);
	final JLabel rLbl = new JLabel(Room.name(), JLabel.LEFT);
	final OverloadedKnob lKnob;
	final OverloadedKnob rKnob;

	@Getter final UpdatePanel left;
	@Getter final UpdatePanel right;

	ReverbPlus(Channel ch) {
		this.ch = ch;
		this.reverb = ch.getReverb();
		lKnob = new OverloadedKnob(ch, reverb, Wet.ordinal(), Damp.ordinal());
		rKnob = new OverloadedKnob(ch, reverb, Room.ordinal(), Width.ordinal());

		rLbl.setFont(Gui.FONT11);
		lLbl.setFont(Gui.FONT11);

		left = new UpdatePanel(lKnob, lLbl);
		right = new UpdatePanel(rKnob, rLbl);
	}

	public void toggle() {

		lLbl.setText( HQ.isShift() ? Damp.name() : Wet.name());
		rLbl.setText( HQ.isShift() ? Width.name() : Room.name());
		update();
	}

	@Override
	public void update() {
		left.update();
		right.update();
	}

	public class UpdatePanel extends Gui.Opaque implements Updateable {

		@Getter private final OverloadedKnob knob;

		public UpdatePanel(OverloadedKnob knob, JLabel label) {
			this.knob = knob;
			add(knob);
			add(label);
		}

		@Override public void update() {
			knob.update();
			Color target = ch.isActive(reverb) ? Pastels.getFx(Reverb.class) : null;
			if (getBackground() != target)
				setBackground(target);
		}

	}



}
