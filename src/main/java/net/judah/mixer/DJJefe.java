package net.judah.mixer;

import java.util.ArrayList;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.judah.JudahZone;
import net.judah.looper.Loop;

public class DJJefe extends JPanel {

	private final ArrayList<ChannelFader> channels = new ArrayList<ChannelFader>();
	
    public DJJefe() {
        for (Loop loop : JudahZone.getLooper().getLoops()) 
        	channels.add(loop.getFader());
        for (Channel channel : JudahZone.getChannels()) 
        	channels.add(channel.getFader());
        channels.add(JudahZone.getMasterTrack().getFader());
        
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        add(new JLabel(" ")); // padding

        int quad = 0;
        for (ChannelFader fader : channels) {
        	if (quad++ % 4 == 0) 
        		add(new JLabel(" ")); // spacer
        	add(fader);
        }

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
