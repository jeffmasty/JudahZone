package net.judah.widgets;

import javax.swing.JSlider;

import net.judah.looper.Loop;

public class LoopWidget extends Slider {
	
	private final Loop main;
	public LoopWidget(Loop sync) {
		super(null);
		main = sync;
		sync.setFeedback(this);
		setOrientation(JSlider.HORIZONTAL);
        setEnabled(false);
	}
	
	public void update() {
		Integer length = main.getLength();
		if (length == null) {
			if (getValue() != 0)
				setValue(0);
		}
		else {
			int val = (int) Math.floor(100 * main.getTapeCounter().intValue() / main.getLength());
			if (val != getValue()) 
				setValue(val);
		}		
	}

}
