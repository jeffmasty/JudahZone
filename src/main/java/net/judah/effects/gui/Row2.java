package net.judah.effects.gui;

import java.awt.Component;
import java.util.ArrayList;

import lombok.RequiredArgsConstructor;
import net.judah.controllers.KnobMode;
import net.judah.mixer.Channel;

@RequiredArgsConstructor
public abstract class Row2 {

	protected static final int KNOBS = 4;
	protected final Channel channel;
	protected final KnobMode mode;
		
	
	public abstract void update();
	
	public abstract ArrayList<Component> getControls();

}
