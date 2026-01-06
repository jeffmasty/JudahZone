package net.judah.gui.widgets;

import judahzone.api.FX;
import judahzone.gui.Updateable;
import judahzone.widgets.RangeSlider;
import net.judah.gui.MainFrame;

public class DoubleSlider extends RangeSlider implements Updateable {

	private final FX lower;
	private final int lowIdx;
	private final FX upper;
	private final int upperIdx;

	public DoubleSlider(FX lower, int lowIdx, FX upper, int upperIdx) {
		this.lower = lower;
		this.lowIdx = lowIdx;
		this.upper = upper;
		this.upperIdx = upperIdx;
		update();
		addChangeListener(e->fireChange());

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
