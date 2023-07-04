package net.judah.gui.knobs;

import java.awt.Dimension;

import javax.swing.JButton;

import lombok.Getter;
import net.judah.gui.Pastels;
import net.judah.gui.Size;
import net.judah.midi.JudahClock;
import net.judah.seq.track.Cycle;
import net.judah.seq.track.MidiTrack;

public class TrackPattern extends JButton {
	private static Dimension d = new Dimension(Size.STD_HEIGHT, Size.STD_HEIGHT);

	@Getter private final int frame;
	private final MidiTrack track;
	
	public TrackPattern(MidiTrack t, int i) {
		super("" + (i + 1));
		this.frame = i;
		this.track = t;
		setMaximumSize(d);
		setPreferredSize(d);
		setOpaque(true);
		addActionListener(e->track.toFrame(frame));
	}
	
	public void update() {
		if (Math.ceil(track.getCurrent() / 2) == frame)
			setBackground(Pastels.GREEN);
		else if (track.getCycle() == Cycle.ALL) 
			setBackground(null);
		else { // blue if frame inside Cycle
			int start = track.getLaunch() / 2 + track.getOffset();
			int end = track.getCycle() == Cycle.CLCK ? JudahClock.getLength() / 2: track.getCycle().getLength() / 2;
			if (frame >= start && frame < start + end)
				setBackground(Pastels.BLUE);
			else 
				setBackground(null);
		}
	}
	
}
