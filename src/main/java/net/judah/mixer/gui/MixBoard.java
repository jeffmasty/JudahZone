package net.judah.mixer.gui;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.swing.BoxLayout;
import javax.swing.JSeparator;

import net.judah.JudahZone;
import net.judah.looper.Loop;
import net.judah.metronome.Metronome;
import net.judah.mixer.Channel;
import net.judah.util.Tab;

public class MixBoard extends Tab {
	
	private final class MySeparator extends JSeparator {
		public MySeparator() { setPreferredSize(new Dimension(350, 11)); } }
	
	private final ArrayList<ChannelGui> channels = new ArrayList<ChannelGui>();
	private final Metronome metro = new Metronome();
	
	public MixBoard() {
		super(true);
		JudahZone.getServices().add(metro);
	}
	
	public void update() {
		for (ChannelGui ch : channels) 
			ch.update();
	}
	
	
	
	public void setup(List<Channel> channels2, List<Loop> loops) {
		
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		add(new MySeparator());
		
		// ChannelGui master = new ChannelGui(new Channel(new Instrument("Master", InstType.Other, null, null)));
		// channels.add(master); add(master);
		
		for (Loop loop : loops) {
			ChannelGui gui = new ChannelGui(new Channel(loop));
			channels.add(gui);
			add(gui);
		}

		add(new MySeparator());
		
		for (Channel c : channels2) {
			ChannelGui gui = new ChannelGui(c);
			channels.add(gui);
			add(gui);
		}
		
//		Properties props = new Properties();
//		props.put(MixerCommands.GAIN_PROP, "todo");
//		props.put(MixerCommands.CHANNEL_PROP, 0);
//		mappings.add(new Mapping(command, MPK.knob(0, 4), props, DYNAMIC));
//		props = new Properties();
//		props.put(MixerCommands.GAIN_PROP, "todo");
//		props.put(MixerCommands.CHANNEL_PROP, 1);
//		mappings.add(new Mapping(command, MPK.knob(0, 5), props, DYNAMIC));
//		props = new Properties();
//		props.put(MixerCommands.GAIN_PROP, "todo");
//		props.put(MixerCommands.CHANNEL_PROP, 2);
//		mappings.add(new Mapping(command, MPK.knob(0, 6), props, DYNAMIC));
//		props = new Properties();
//		props.put(MixerCommands.GAIN_PROP, "todo");
//		props.put(MixerCommands.CHANNEL_PROP, 3);
//		mappings.add(new Mapping(command, MPK.knob(0, 7), props, DYNAMIC));

		
		
		add(new MySeparator());
		add(new MySeparator());
		
		add(metro);
		add(new MySeparator());
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {

	}
	
	@Override public String getTabName() { return "Mixer"; }
	@Override public void setProperties(Properties p) {  }

}


		