package net.judah.tracker.view;

import javax.swing.JButton;

import lombok.Getter;
import net.judah.tracker.Pattern;
import net.judah.tracker.Track;

public class PatternBtn extends JButton {

	@Getter private final Pattern pattern;
	private final Track track;
	
	public PatternBtn(Track t, Pattern p) {
		super(p.getName().substring(0, 1));
		setToolTipText(p.getName());
		this.pattern = p;
		this.track = t;
		addActionListener(e->track.setCurrent(pattern));
	}
	
}
