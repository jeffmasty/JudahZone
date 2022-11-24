package net.judah.tracker.view;

import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.swing.JPanel;

import net.judah.tracker.Pattern;
import net.judah.tracker.Track;
import net.judah.util.Pastels;
import net.judah.util.Size;

public class PatternBox extends JPanel {
	private final Track track;
	
	public PatternBox(Track t) {
		setLayout(new FlowLayout(FlowLayout.LEFT, 1, 1));
		this.track = t;
		setPreferredSize(new Dimension(Size.WIDTH_SONG / 4, Size.STD_HEIGHT));
		fill();
	}

	public void update() {
		Pattern current = track.getCurrent();
		for (int i = 0; i < getComponentCount(); i++) {
			PatternBtn btn = (PatternBtn)getComponent(i);
			btn.setBackground(btn.getPattern() == current ? Pastels.GREEN : Pastels.MY_GRAY);
		}
	}

	public void fill() {
		removeAll();
		for (Pattern p : track) 
			add(new PatternBtn(track, p));
		update();
	}
	
}
