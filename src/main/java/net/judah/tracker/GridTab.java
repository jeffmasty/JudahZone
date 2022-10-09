package net.judah.tracker;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

import net.judah.JudahZone;

public class GridTab extends JPanel {

	public GridTab() {
		setName("BeatBox");
		setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		Tracker tracks = JudahZone.getTracker();
		if (tracks.getCurrent() != null && tracks.getCurrent().getEdit() != null)
			add(tracks.getCurrent().getEdit());
	}
	
	public void changeTrack(Track t) {
		removeAll();
		add(t.getEdit());
		t.getEdit().repaint();
	}
	
	
	
}
