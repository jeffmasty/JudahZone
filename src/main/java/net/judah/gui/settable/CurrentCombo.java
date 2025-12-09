package net.judah.gui.settable;

import net.judah.gui.Gui;
import net.judah.gui.Size;
import net.judah.gui.widgets.Integers;
import net.judah.omni.Threads;
import net.judah.seq.track.Computer;

public class CurrentCombo extends SetCombo<Integer> {

	private final Computer track;

	public CurrentCombo(Computer t) {
		super(Integers.generate(1, 100), 1);
		this.track = t;
		Gui.resize(this, Size.MICRO);
	}

	@Override
	protected void action() {
		if (getSelectedIndex() !=  track.getFrame())
			track.toFrame(getSelectedIndex());
	}

	public void update() {
		if (this != set && getSelectedIndex() != track.getFrame())
			Threads.execute(()->{override(1 + track.getFrame());});
	}


}
