package net.judah.gui.widgets;

import net.judah.fx.Effect;
import net.judah.gui.MainFrame;
import net.judah.gui.Updateable;

public class DoubleSlider extends RangeSlider implements Updateable {

	private final Effect lower;
	private final int lowIdx;
	private final Effect upper;
	private final int upperIdx;

	public DoubleSlider(Effect lower, int lowIdx, Effect upper, int upperIdx) {
		this.lower = lower;
		this.lowIdx = lowIdx;
		this.upper = upper;
		this.upperIdx = upperIdx;
		addChangeListener(l->fireChange());
		update();
	}

	private void fireChange() {
		lower.set(lowIdx, getValue());
		upper.set(upperIdx, getValue() + getExtent());
		MainFrame.update(this);
	}

	@Override public void update() {
		int low = lower.get(lowIdx);
		if (getValue() != low)
			setValue(low);
		int hi = upper.get(upperIdx);
		int extent = hi - low;
		if (getExtent() != extent)
			setExtent(extent);
	}

}
