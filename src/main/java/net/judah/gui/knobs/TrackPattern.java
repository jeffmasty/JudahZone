package net.judah.gui.knobs;

import java.awt.Dimension;

import javax.swing.JButton;

import lombok.Getter;
import net.judah.gui.Pastels;
import net.judah.gui.Size;
import net.judah.seq.MidiTrack;

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
		addActionListener(e->track.setFrame(frame));
	}
	
	public void update() {
		setBackground(track.getFrame() == frame ? Pastels.GREEN : null);
	}
	
}
