package net.judah.gui.fx;

import lombok.Getter;
import net.judah.fx.Effect;
import net.judah.gui.HQ;
import net.judah.gui.Updateable;
import net.judah.gui.widgets.FxKnob;
import net.judah.gui.widgets.Knob;
import net.judah.gui.widgets.Knob.KnobListener;
import net.judah.seq.MidiConstants;


@Getter
public class OverloadedKnob extends Knob implements KnobListener, Updateable {

	private final Effect fx;
	private final int main;
	private final int alt;

	OverloadedKnob(Effect target, int normal, int shift) {
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
			fx.setActive(val > MidiConstants.THOLD_LO);
		}
	}

	public void knob(boolean up) {
		int target = FxKnob.offset(fx.get(HQ.isShift() ? alt : main), up);
		knobChanged(target);
	}


}

