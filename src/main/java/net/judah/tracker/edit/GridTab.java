package net.judah.tracker.edit;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

import net.judah.tracker.Track;
import net.judah.tracker.Tracker;

public class GridTab extends JPanel {

	public GridTab(Tracker tracker) {
		setName("BeatBox");
		setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		if (tracker.getCurrent() != null && tracker.getCurrent().getEdit() != null)
		add(tracker.getCurrent().getEdit());
		
	}
	
	public void changeTrack(Track t) {
		removeAll();
		add(t.getEdit());
		t.getEdit().repaint();
	}
	
}
