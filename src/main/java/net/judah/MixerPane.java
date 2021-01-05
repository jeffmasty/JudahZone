package net.judah;

import java.awt.GridLayout;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import lombok.Getter;
import net.judah.looper.Sample;
import net.judah.mixer.Channel;
import net.judah.mixer.ChannelPanel;
import net.judah.song.SonglistTab;
import net.judah.util.Constants;

public class MixerPane extends JPanel {
	
	public static final int WIDTH = 360;
	
	@Getter private static MixerPane instance; 
	private final JComponent songlist;

	
	private JPanel knobs = new JPanel();
	private JPanel looper = new JPanel();
	private final ChannelPanel highlight = new ChannelPanel();
	
	public MixerPane() {
		instance = this;
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));		

		add(highlight);
		
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
		
		add(JudahZone.getPlugins().getGui());
		
		setSize(WIDTH, getPreferredSize().height);
		highlight.setChannel(JudahZone.getChannels().getGuitar());
	}

	public void setFocus(Channel channel) {
		highlight.setChannel(channel);
	}

	public void addSample(Sample s) {
		looper.add(s.getGui());
	}

	public void removeSample(Sample s) {
		looper.remove(s.getGui());
	}

}
