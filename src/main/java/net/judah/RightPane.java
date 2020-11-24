package net.judah;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;

import net.judah.sequencer.Sequencer;
import net.judah.song.SonglistTab;

public class RightPane extends JPanel {
	
	public static final int WIDTH = 325;
	
	private final JComponent songlist;
	
	public RightPane() {
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		songlist = new SonglistTab(JudahZone.defaultSetlist);
	}

	public void setSong(Sequencer sequencer) {
		removeAll();
		add(sequencer.getMixer().getGui());
		add(songlist);
		setSize(WIDTH, getPreferredSize().height);
	}

}
