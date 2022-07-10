package net.judah.looper;

import java.awt.Dimension;

import lombok.Getter;
import net.judah.util.RainbowFader;

public class LoopWidget extends RainbowFader {
	
	@Getter private static LoopWidget instance;
	private final Loop loop;
	
	public LoopWidget(Loop a) {
		super(e ->{});
		instance = this;
		loop = a;
		setOrientation(RainbowFader.HORIZONTAL);
        setPreferredSize(new Dimension(180, 30));
        setEnabled(false);
	}
	
	public void update() {
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
