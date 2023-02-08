package net.judah.gui.settable;

import java.util.ArrayList;

import net.judah.seq.MidiTrack;
import net.judah.util.Constants;

public class Launch extends SetCombo<Integer> {

	private static final ArrayList<Launch> instances = new ArrayList<>();
	private final MidiTrack track;
	
	public Launch(MidiTrack t) {
		super(Bar.framez, 1);
		track = t;
		instances.add(this);
	}
	
	@Override
	protected void action() {
		if (false == getSelectedItem() instanceof Integer)
			return;
		track.setLaunch(-1 + (int)getSelectedItem());
	}

	public static void update(MidiTrack t) {
		Constants.execute(()->{
			int launch = t.getLaunch() + 1;
			for (Launch update : instances)
				if (update.track == t && (int)update.getSelectedItem() != launch)
					update.override(launch);
		});
	}
	
}
