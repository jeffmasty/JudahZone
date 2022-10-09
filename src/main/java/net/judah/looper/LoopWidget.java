package net.judah.looper;

import java.awt.Dimension;

import javax.swing.JSlider;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.util.Slider;

public class LoopWidget extends Slider {
	
	@Getter private static LoopWidget instance;
	
	public LoopWidget() {
		super(null);
		instance = this;
		setOrientation(JSlider.HORIZONTAL);
        setPreferredSize(new Dimension(180, 30));
        setEnabled(false);
	}
	
	public void update() {
		Loop loop = JudahZone.getLooper().getLoopA();
		Integer length = loop.getLength();
		if (length == null) {
			if (getValue() != 0)
				setValue(0);
		}
		else {
			int val = (int) Math.floor(100 * loop.getTapeCounter().intValue() / loop.getLength());
			if (val != getValue()) 
				setValue(val);
		}		
	}

}
