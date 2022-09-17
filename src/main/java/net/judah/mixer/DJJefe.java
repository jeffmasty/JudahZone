package net.judah.mixer;

import java.awt.GridLayout;
import java.util.ArrayList;

import javax.swing.JPanel;

import net.judah.JudahZone;
import net.judah.looper.Loop;
import net.judah.util.Constants;

/** Mixer GUI for audio input and output faders */
public class DJJefe extends JPanel {

	private final ArrayList<ChannelFader> channels = new ArrayList<ChannelFader>();
	
    public DJJefe() {
        setLayout(new GridLayout(1, channels.size()));

    	for (Loop loop : JudahZone.getLooper()) 
        	channels.add(loop.getFader());
        for (Channel channel : JudahZone.getChannels()) 
        	channels.add(channel.getFader());
        channels.add(JudahZone.getMains().getFader());
        
        for (ChannelFader fader : channels) 
        	add(fader);

        doLayout();
    }

	public void update(Channel channel) {
		for (ChannelFader ch : channels) 
			if (ch.getChannel().equals(channel)) 
				ch.update();
	}

	public void updateAll() {
		for (ChannelFader ch : channels) 
			ch.update();
	}

	public void highlight(Channel o) {
		for (ChannelFader ch : channels) 
			ch.setBorder(ch.getChannel() == o ? Constants.Gui.HIGHLIGHT : Constants.Gui.NO_BORDERS);
	}
}
