package net.judah.gui.fx;

import judahzone.api.Effect;
import lombok.Getter;
import net.judah.gui.HQ;
import net.judah.gui.widgets.FxKnob;
import net.judah.gui.widgets.Knob;
import net.judah.gui.widgets.Knob.KnobListener;
import net.judah.mixer.Channel;
import net.judah.seq.MidiConstants;
import net.judahzone.gui.Updateable;


@Getter
public class OverloadedKnob extends Knob implements KnobListener, Updateable {

	private final Channel ch;
	private final Effect fx;
	private final int main;
	private final int alt;

	OverloadedKnob(Channel ch, Effect target, int normal, int shift) {
		this.ch = ch;
		this.fx = target;
		this.main = normal;
		this.alt = shift;
		setKnobColor(FxKnob.THUMB);
		addListener(this);
		setOpaque(true);
	}

	@Override public void update() {
		int target = fx.get(HQ.isShift() ? alt : main);
		if (getValue() != target)
			setValue(target);
	}

	@Override public void knobChanged(int val) {
		if (HQ.isShift())
			fx.set(alt,  val);
		else {
			fx.set(main, val);

			ch.setActive(fx, val > MidiConstants.THOLD_LO);
		}
	}

	public void knob(boolean up) {
		int target = FxKnob.offset(fx.get(HQ.isShift() ? alt : main), up);
		knobChanged(target);
	}


}

