package net.judah.gui.settable;

import java.util.HashSet;

import net.judah.seq.CYCLE;
import net.judah.seq.MidiTrack;

public class Cycle extends SetCombo<CYCLE> {
	
	private static final HashSet<Cycle> instances = new HashSet<>();
	private final MidiTrack track;
	
	public Cycle(MidiTrack t) {
		super(CYCLE.values(), t.getCycle());
		track = t;
		instances.add(this);
	}

	@Override
	protected void action() {
		CYCLE c = (CYCLE)getSelectedItem();
		if (track.getCycle() != c)
			track.setCycle(c);
		update(track);
	}
	
	public static void update(MidiTrack t) {
		for (Cycle c : instances)
			if (c.track == t && c.getSelectedItem() != t.getCycle())
				c.override(t.getCycle());
	}
	
}
