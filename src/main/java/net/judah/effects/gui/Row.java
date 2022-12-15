package net.judah.effects.gui;

import java.awt.Component;
import java.util.ArrayList;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.judah.mixer.Channel;
import net.judah.widgets.JudahKnob;

@RequiredArgsConstructor
public class Row {

	protected final Channel channel;
	
	@Getter protected final ArrayList<Component> controls = new ArrayList<>();
	
	public final void update() {
    	for (Component c : getControls()) 
			if (c instanceof JudahKnob)
				((JudahKnob)c).update();
			else if (c instanceof FxCombo)
				((FxCombo)c).update();
	}

}
