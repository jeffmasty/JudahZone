package net.judah.gui.settable;

import java.util.ArrayList;

import net.judah.seq.Cue;
import net.judah.seq.MidiTrack;
import net.judah.util.Constants;

public class CueCombo extends SetCombo<Cue> {

	private static final ArrayList<CueCombo> instances = new ArrayList<>();
	private final MidiTrack track;
	
	public CueCombo(MidiTrack t) {
		super(Cue.values(), t.getCue());
		this.track = t;
		instances.add(this);
	}
	
	@Override
	protected void action() {
		track.setCue((Cue)getSelectedItem());
	}

	// TODO hookup
	public static void refresh(MidiTrack t) {
		Constants.execute(()->{
		for (CueCombo c : instances)
			if (c.track == t)
				c.override(t.getCue());
		});
	}
	
}
