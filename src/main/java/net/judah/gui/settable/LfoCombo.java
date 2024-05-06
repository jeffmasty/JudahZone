package net.judah.gui.settable;

import net.judah.fx.LFO;
import net.judah.fx.LFO.Target;
import net.judah.gui.Updateable;
import net.judah.mixer.Channel;

public class LfoCombo extends SetCombo<Target> implements Updateable {

	private final Channel ch;
	
	public LfoCombo(Channel channel) {
		super(LFO.Target.values(), channel.getLfo().getTarget());
		this.ch = channel;
	}
	
	@Override
	protected void action() {
		ch.getLfo().setTarget((Target)getSelectedItem());
	}

	@Override
	public void update() {
		if (set == this)
			return;
		if (ch.getLfo().getTarget() != getSelectedItem())
			override(ch.getLfo().getTarget());
	}
	
}
