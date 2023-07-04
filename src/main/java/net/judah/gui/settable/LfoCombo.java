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
		if (set == this) return;
		Target selected = (Target)getSelectedItem();
		if (ch.getLfo().getTarget() != selected)
			ch.getLfo().setTarget(selected);
	}

	@Override
	public void update() {
		if (ch.getLfo().getTarget() != getSelectedItem())
			override(ch.getLfo().getTarget());
	}
	
}
