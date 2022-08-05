package net.judah.tracker;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

public class GridTab extends JPanel {

	
	public GridTab() {
		setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		if (Tracker.getCurrent() != null && Tracker.getCurrent().getGrid() != null)
			add(Tracker.getCurrent().getGrid());
	}
	
	public void changeTrack(Track t) {
		removeAll();
		add(t.getGrid());
		t.getGrid().repaint();
	}
	
}
