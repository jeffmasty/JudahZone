package net.judah.gui.settable;

import java.util.HashSet;

import net.judah.gui.Gui;
import net.judah.gui.Size;
import net.judah.seq.CYCLE;
import net.judah.seq.Computer;
import net.judah.util.Constants;

public class Cycle extends SetCombo<CYCLE> {
	
	private static final HashSet<Cycle> instances = new HashSet<>();
	private final Computer track;
	
	public Cycle(Computer track) {
		super(CYCLE.values(), track.getCycle());
		this.track = track;
		instances.add(this);
		Gui.resize(this, Size.SMALLER_COMBO);
	}

	@Override protected void action() {
		CYCLE c = (CYCLE)getSelectedItem();
		if (track.getCycle() != c)
			track.setCycle(c);
	}
	
	public static void update(Computer t) {
		Constants.execute(()->{
			for (Cycle c : instances)
				if (c.track == t && c.getSelectedItem() != t.getCycle())
					c.override(t.getCycle());
		});
	}
	
}
