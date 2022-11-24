package net.judah.effects.gui;

import java.awt.Component;
import java.util.ArrayList;

import lombok.RequiredArgsConstructor;
import net.judah.mixer.Channel;
import net.judah.util.JudahKnob;

@RequiredArgsConstructor
public abstract class Row {

	protected static final int KNOBS = 4;
	protected final Channel channel;
	
	public final void update() {
    	for (Component c : getControls()) 
			if (c instanceof JudahKnob)
				((JudahKnob)c).update();
			else if (c instanceof FxCombo)
				((FxCombo)c).update();
	}
	
	public abstract ArrayList<Component> getControls();

}
