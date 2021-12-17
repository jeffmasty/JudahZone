package net.judah;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.judah.mixer.Channel;
import net.judah.mixer.DJJefe;

public class MixerView extends JPanel {

	private final ArrayList<DJJefe> channels = new ArrayList<DJJefe>();
	
    public MixerView(Rectangle bounds) {
        setBounds(bounds);
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        add(new JLabel(" ")); // padding

        channels.add((DJJefe)add(new DJJefe(JudahZone.getMasterTrack())));
        
        for (Channel channel : JudahZone.getLooper()) {
        	channels.add((DJJefe)add(new DJJefe(channel)));
        }
        
    assert channels.size() > 4 : Arrays.toString(channels.toArray());
        //mixer.add(Box.createHorizontalStrut(15));
        for (Channel channel : JudahZone.getChannels()) {
        	channels.add((DJJefe)add(new DJJefe(channel)));
        }
        
        add(new JLabel(" "));
        doLayout();
    }

	public void update(Channel channel) {
		for (DJJefe ch : channels) {
			if (ch.getChannel().equals(channel)) {
				ch.update();
			}
		}
	}

	public void updateAll() {
		for (DJJefe ch : channels) {
			ch.update();
		}
	}
}
