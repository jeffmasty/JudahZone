package net.judah.gui.settable;

import net.judah.fx.LFO;
import net.judah.fx.LFO.Target;
import net.judah.mixer.Channel;

public class Lfo extends SetCombo<Target> {

	private final Channel ch;
	
	public Lfo(Channel channel) {
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

}
