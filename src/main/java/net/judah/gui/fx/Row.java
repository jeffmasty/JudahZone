package net.judah.gui.fx;

import java.awt.Component;
import java.util.ArrayList;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.judah.gui.Updateable;
import net.judah.mixer.Channel;

@RequiredArgsConstructor
public class Row implements Updateable {

	protected final Channel channel;
	
	@Getter protected final ArrayList<Component> controls = new ArrayList<>();
	
	@Override
	public final void update() {
    	for (Component c : controls) 
			if (c instanceof Updateable)
				((Updateable)c).update();
	}

}
