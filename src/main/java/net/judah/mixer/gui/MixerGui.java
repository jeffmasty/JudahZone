package net.judah.mixer.gui;

import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JComponent;

import net.judah.JudahZone;
import net.judah.looper.Sample;
import net.judah.mixer.Channel;
import net.judah.sequencer.SeqDisplay;

public class MixerGui extends JComponent {
	  
	private final ArrayList<ChannelGui> channels = new ArrayList<ChannelGui>();
	
	public MixerGui(List<Sample> loops, SeqDisplay seq) {
		
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
//		add(metro.getGui());
		add(seq);
		
		for (Channel c : JudahZone.getChannels()) {
			ChannelGui gui = new ChannelGui(c);
			channels.add(gui);
			add(gui);
		}

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
	
}


		