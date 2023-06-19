package net.judah.gui.settable;

import java.awt.Color;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import net.judah.JudahZone;
import net.judah.gui.widgets.Integers;
import net.judah.seq.MidiTrack;
import net.judah.util.Constants;
import net.judah.util.RTLogger;

public class Launch extends Integers {
	public static final int MAX = 99;
	
	private static final ArrayList<Launch> instances = new ArrayList<>();
	private final MidiTrack track;
	private final ActionListener listener;
	
	public Launch(MidiTrack t) {
		super(1, 99);
		track = t;
		instances.add(this);
		listener = e->{
			if (track.getLaunch() != getSelectedIndex())
				track.setLaunch(getSelectedIndex());};
		addActionListener(listener);
	}
	
	public static void update(MidiTrack t) {
		if (t.getLaunch() >= MAX) {
			RTLogger.log(Launch.class, t.getName() + " launch too large: " + t.getLaunch() + " scene " + JudahZone.getSongs().getCurrent());
			return;
		}
		Constants.execute(()->{
			for (Launch update : instances) {
				if (update.track != t) continue;
				if (update.getSelectedIndex() != t.getLaunch() && t.getLaunch() < MAX)
					update.setSelectedIndex(t.getLaunch());
				update.setBackground(update.getSelectedIndex() % 2 == 0 ? null : Color.YELLOW);
			}
		});
	}
	
}
