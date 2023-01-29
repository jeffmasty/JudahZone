package net.judah.gui.settable;

import java.util.HashSet;

import net.judah.seq.MidiConstants;
import net.judah.seq.MidiTrack;

public class Bar extends SetCombo<Integer> {

	private static Integer[] framez = new Integer[MidiConstants.MAX_FRAMES];
	static {for (int i = 0; i < framez.length; i++) framez[i] = i;}
	private static final HashSet<Bar> instances = new HashSet<>();
	
	private final MidiTrack track;
	
	public Bar(MidiTrack t) {
		super(framez, 0);
		this.track = t;
		instances.add(this);
	}
	
	@Override
	protected void action() {
		if (getSelectedItem() != null)
			track.setFrame((int)getSelectedItem());
		for (Bar update : instances)
			if (this != update && update.track == track)
				update.override(track.getFrame());
	}

	public static void update(MidiTrack t) {
		for (Bar c : instances)
			if (c.track == t)
				if (t.getFrame() != (int)c.getSelectedItem())
					c.override(t.getFrame());
	}
	
}
