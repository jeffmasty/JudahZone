package net.judah.mixer;

import java.awt.Rectangle;
import java.util.ArrayList;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.judah.JudahZone;
import net.judah.looper.Sample;

public class DJJefe extends JPanel {

	private final ArrayList<ChannelFader> channels = new ArrayList<ChannelFader>();
	
    public DJJefe(Rectangle bounds) {
        channels.add(JudahZone.getMasterTrack().getFader());
        for (Sample loop : JudahZone.getLooper().getLoops()) 
        	channels.add(loop.getFader());
        for (Channel channel : JudahZone.getChannels()) 
        	channels.add(channel.getFader());
        
    	setBounds(bounds);
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        add(new JLabel(" ")); // padding

        for (ChannelFader fader : channels) 
        	add(fader);

        add(new JLabel(" ")); // padding
        doLayout();
    }

	public void update(Channel channel) {
		for (ChannelFader ch : channels) {
			if (ch.getChannel().equals(channel)) {
				ch.update();
			}
		}
	}

	public void updateAll() {
		for (ChannelFader ch : channels) {
			ch.update();
		}
	}
}
