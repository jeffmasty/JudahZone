package net.judah.seq;

import java.awt.Rectangle;

import javax.swing.JPanel;

import lombok.Getter;
import net.judah.midi.JudahClock;

public abstract class MusicGrid extends JPanel {
	
	protected final MidiTrack track;
	protected final JudahClock clock;
	@Getter protected final Measure scroll;
	
	public MusicGrid(MidiTrack t, Rectangle r) {
		this.track = t;
		this.clock = track.getClock();
		scroll = new Measure(track);
		setBounds(r);
		setMaximumSize(r.getSize());
		setPreferredSize(r.getSize());
	}
	
	public abstract Musician getMusician();
	
	public abstract void timeSig();

}
