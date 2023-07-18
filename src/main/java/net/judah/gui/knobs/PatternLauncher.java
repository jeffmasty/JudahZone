package net.judah.gui.knobs;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;

import javax.swing.JPanel;
import javax.swing.border.LineBorder;

import net.judah.gui.Gui;
import net.judah.gui.Size;
import net.judah.seq.track.MidiTrack;

public class PatternLauncher extends JPanel {
	private static final int HEIGHT = Size.STD_HEIGHT * 2 + 26;
	
	private final MidiTrack track;
	private final ArrayList<TrackPattern> patterns = new ArrayList<>();
	
	public PatternLauncher(MidiTrack track) {
		this.track = track;
		setLayout(new GridLayout(2, 8));
		Dimension holderSz = new Dimension(Size.WIDTH_KNOBS - 14, HEIGHT);
		Gui.resize(this, holderSz);
		setOpaque(true);
		update();
		setBorder(new LineBorder(Color.GRAY));
	}

	public void update() {
		if (track.frames() != patterns.size())
			fill();
		for (int i = 0; i < patterns.size(); i++)
			patterns.get(i).update();
	}
	
	public void fill() {
		removeAll();
		patterns.clear();
		for (int i = 0; i < track.frames(); i++) {
			TrackPattern p = new TrackPattern(track, i); 
			patterns.add(p);
			add(p);
		}
		invalidate();
	}
	
}
