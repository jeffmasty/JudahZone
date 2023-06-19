package net.judah.gui.settable;

import java.util.ArrayList;

import net.judah.seq.MidiConstants;
import net.judah.seq.MidiTrack;
import net.judah.util.Constants;

public class Bar extends SetCombo<Integer> {

	static Integer[] framez = new Integer[MidiConstants.MAX_FRAMES];
	static {for (int i = 1; i <= framez.length; i++) framez[i-1] = i;}
	
	private static final ArrayList<Bar> instances = new ArrayList<>();
	private final MidiTrack track;
	
	public Bar(MidiTrack t) {
		super(framez, 1);
		this.track = t;
		instances.add(this);
	}
	
	@Override
	protected void action() {
		if (set == this) return;
		if (getSelectedIndex() !=  track.getCurrent())
			track.setCurrent(getSelectedIndex());
	}

	public static void update(MidiTrack t) {
		Constants.execute(()->{
			int frame = t.getCurrent();
			for (Bar update : instances)
				if (update.track == t && update.getSelectedIndex() != frame)
					update.override(frame + 1);
		});
	}
	
}
