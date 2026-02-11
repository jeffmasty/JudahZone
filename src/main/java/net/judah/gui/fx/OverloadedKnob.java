package net.judah.gui.fx;

import judahzone.api.FX;
import judahzone.api.MidiConstants;
import judahzone.gui.Updateable;
import judahzone.widgets.Knob;
import judahzone.widgets.Knob.KnobListener;
import lombok.Getter;
import net.judah.channel.Channel;
import net.judah.gui.ShiftBtn;
import net.judah.gui.widgets.FxKnob;


@Getter
public class OverloadedKnob extends Knob implements KnobListener, Updateable {

	private final Channel ch;
	private final FX fx;
	private final int main;
	private final int alt;

	OverloadedKnob(Channel ch, FX target, int normal, int shift) {
		this.ch = ch;
		this.fx = target;
		this.main = normal;
		this.alt = shift;
		// setKnobColor(FxKnob.THUMB);
		addListener(this);
		setOpaque(true);
	}

	@Override public void update() {
		int target = fx.get(ShiftBtn.isActive() ? alt : main);
		if (getValue() != target)
			setValue(target);
	}

	@Override public void knobChanged(int val) {
		if (ShiftBtn.isActive())
			fx.set(alt,  val);
		else {
			fx.set(main, val);

			ch.setActive(fx, val > MidiConstants.THOLD_LO);
		}
	}

	public void knob(boolean up) {
		int target = FxKnob.offset(fx.get(ShiftBtn.isActive() ? alt : main), up);
		knobChanged(target);
	}


}

