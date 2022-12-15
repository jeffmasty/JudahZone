package net.judah.gui.player;

import java.awt.Dimension;

import javax.swing.JButton;

import lombok.Getter;
import net.judah.gui.Pastels;
import net.judah.gui.Size;
import net.judah.seq.Bar;
import net.judah.seq.MidiTrack;

public class BarStool extends JButton {
	private static Dimension d = new Dimension(Size.STD_HEIGHT, Size.STD_HEIGHT);

	@Getter private final Bar bar;
	private final MidiTrack track;
	
	public BarStool(MidiTrack t, Bar b) {
		super("" + t.indexOf(b));
		this.bar = b;
		this.track = t;
		setMaximumSize(d);
		setPreferredSize(d);
		setOpaque(true);
		addActionListener(e->track.setCurrent(track.indexOf(b)));
	}
	
	public void update() {
		setBackground(track.getState().current == track.indexOf(bar)? Pastels.GREEN : null);
	}
	
}
