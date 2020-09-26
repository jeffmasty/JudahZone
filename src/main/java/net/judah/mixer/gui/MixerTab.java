package net.judah.mixer.gui;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.swing.BoxLayout;
import javax.swing.JSeparator;

import net.judah.JudahZone;
import net.judah.looper.Sample;
import net.judah.mixer.Channel;
import net.judah.sequencer.Metronome;
import net.judah.util.Tab;

public class MixerTab extends Tab {
	  
	private final ArrayList<ChannelGui> channels = new ArrayList<ChannelGui>();
	private Metronome metro;
	
	private final class MySeparator extends JSeparator {
		public MySeparator() { 
			setPreferredSize(new Dimension(320, 11));
			setMinimumSize(new Dimension(75, 11));
		} }
	
	
	public MixerTab(List<Sample> loops, Metronome metronome) {
		super(true);
		this.metro = metronome;
		
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		add(metro);
		for (Channel c : JudahZone.getChannels()) {
			ChannelGui gui = new ChannelGui(c);
			channels.add(gui);
			add(gui);
		}
		
		add(new MySeparator());
		
		for (Sample loop : loops) {
			ChannelGui gui = new ChannelGui(new Channel(loop));
			channels.add(gui);
			add(gui);
		}
		
	}
	
	public void update() {
		for (ChannelGui ch : channels) 
			ch.update();
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
	}
	@Override public String getTabName() { return "Mixer"; }
	@Override public void setProperties(Properties p) {  }

}


		