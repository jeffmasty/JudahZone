package net.judah.gui.settable;

import java.util.ArrayList;

import net.judah.seq.CUE;
import net.judah.seq.MidiTrack;
import net.judah.util.Constants;

public class Cue extends SetCombo<CUE> {

	private static final ArrayList<Cue> instances = new ArrayList<>();
	private final MidiTrack track;
	
	public Cue(MidiTrack t) {
		super(CUE.values(), t.getCue());
		this.track = t;
		instances.add(this);
	}
	
	@Override
	protected void action() {
		track.setCue((CUE)getSelectedItem());
	}

	// TODO hookup
	public static void update(MidiTrack t) {
		Constants.execute(()->{
		for (Cue c : instances)
			if (c.track == t)
				c.override(t.getCue());
		});
	}
	
}
