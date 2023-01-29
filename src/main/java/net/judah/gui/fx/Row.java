package net.judah.gui.fx;

import java.awt.Component;
import java.util.ArrayList;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.judah.gui.settable.Fx;
import net.judah.gui.widgets.FxKnob;
import net.judah.mixer.Channel;

@RequiredArgsConstructor
public class Row {

	protected final Channel channel;
	
	@Getter protected final ArrayList<Component> controls = new ArrayList<>();
	
	public final void update() {
    	for (Component c : getControls()) 
			if (c instanceof FxKnob)
				((FxKnob)c).update();
			else if (c instanceof Fx)
				((Fx)c).update();
	}

}
