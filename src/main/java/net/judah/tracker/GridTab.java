package net.judah.tracker;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

public class GridTab extends JPanel {

	public GridTab(JudahBeatz t) {
		setName("BeatBox");
		setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		if (t.getCurrent() != null && t.getCurrent().getEdit() != null)
			add(t.getCurrent().getEdit());
	}
	
	public void changeTrack(Track t) {
		removeAll();
		add(t.getEdit());
		t.getEdit().repaint();
	}
	
	
	
}
