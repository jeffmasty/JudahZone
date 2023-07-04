package net.judah.gui.settable;

import java.util.ArrayList;

import net.judah.gui.Gui;
import net.judah.gui.Size;
import net.judah.gui.widgets.Integers;
import net.judah.seq.track.Computer;
import net.judah.seq.track.MidiTrack;
import net.judah.util.Constants;

public class CurrentCombo extends SetCombo<Integer> {

	private static final ArrayList<CurrentCombo> instances = new ArrayList<>();
	private final Computer track;
	
	public CurrentCombo(Computer t) {
		super(Integers.generate(1, 100), 1);
		this.track = t;
		instances.add(this);
		Gui.resize(this, Size.MICRO);
	}
	
	@Override
	protected void action() {
		if (set == this) return;
		if (getSelectedIndex() !=  track.getFrame())
			track.toFrame(getSelectedIndex());
	}

	public void update() {
		if (this != set && getSelectedIndex() != track.getFrame())
			Constants.execute(()->{override(1 + track.getFrame());});
	}
	
	public static void update(MidiTrack t) {
		for (CurrentCombo update : instances)
			if (update.track == t && update != set)
				update.update();
	}

}
