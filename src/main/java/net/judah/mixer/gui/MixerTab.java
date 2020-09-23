package net.judah.mixer.gui;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.swing.BoxLayout;
import javax.swing.JSeparator;

import net.judah.looper.Sample;
import net.judah.metronome.Metronome;
import net.judah.mixer.Channel;
import net.judah.util.Tab;

public class MixerTab extends Tab {
	  
	private final class MySeparator extends JSeparator {
		public MySeparator() { setPreferredSize(new Dimension(350, 11)); } }
	private final MySeparator footer = new MySeparator();
	
	private final ArrayList<ChannelGui> channels = new ArrayList<ChannelGui>();
	private Metronome metro;

	public MixerTab() {
		super(true);
	}
	
	public MixerTab(Metronome metronome) {
		super(true);
		this.metro = metronome;
	}
	
	public void update() {
		for (ChannelGui ch : channels) 
			ch.update();
	}
	
	public void setup(List<Channel> channels2, List<Sample> loops) {
		
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		add(new MySeparator());
		
		// ChannelGui master = new ChannelGui(new Channel(new Instrument("Master", InstType.Other, null, null)));
		// channels.add(master); add(master);
		
		for (Channel c : channels2) {
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
		
		add(new MySeparator());
		add(new MySeparator());
		
		if (metro != null) {
			add(metro);
			add(new MySeparator());
		}
	}
	
	public void setMetronome(Metronome metronome) {
		if (this.metro != null) {
			this.metro.close();
			remove(footer);
			remove(this.metro);
		}
		this.metro = metronome;
		if (metronome != null) {
			add(metronome);
			add(footer);
		}
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
	}
	@Override public String getTabName() { return "Mixer"; }
	@Override public void setProperties(Properties p) {  }

}


		