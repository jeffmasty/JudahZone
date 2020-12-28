package net.judah;

import java.awt.GridLayout;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import net.judah.looper.Sample;
import net.judah.mixer.Channel;
import net.judah.song.SonglistTab;
import net.judah.util.Constants;

public class RightPane extends JPanel {
	
	public static final int WIDTH = 360;
	
	private final JComponent songlist;

	private JPanel knobs = new JPanel();
	private JPanel looper = new JPanel();
	
	public RightPane() {
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));		
		
		knobs.setLayout(new GridLayout(0,2));
		for (Channel channel : JudahZone.getChannels()) 
			knobs.add(channel.getGui());
		add(knobs);
		
		looper.setLayout(new GridLayout(0, 1));
		for (Sample loop : JudahZone.getLooper()) 
			looper.add(loop.getGui());
		add(looper); 

        JTabbedPane tabs = new JTabbedPane();
		songlist = new SonglistTab(Constants.defaultSetlist);
		tabs.add("Setlist", songlist);
		tabs.add("Metronome", JudahZone.getMetronome().getGui());
		add(tabs);
		
		setSize(WIDTH, getPreferredSize().height);

	}

}
