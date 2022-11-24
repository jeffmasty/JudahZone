package net.judah.mixer;

import java.awt.GridLayout;
import java.util.ArrayList;

import javax.swing.JPanel;

import lombok.Getter;
import net.judah.looper.Loop;
import net.judah.looper.Looper;
import net.judah.util.Constants;

/** Graphical representation of the Mixer*/
public class DJJefe extends JPanel {

	@Getter private final ArrayList<Channel> channels = new ArrayList<>();
	private final ArrayList<ChannelFader> faders = new ArrayList<ChannelFader>();
	
    public DJJefe(Channel mains, Looper loops, Zone sources) {
        for (Loop loop : loops)
        	addChannel(loop);
    	for (LineIn instrument : sources)
        	addChannel(instrument);
        addChannel(mains);
    	setLayout(new GridLayout(1, channels.size()));
        doLayout();
    }

    public void addChannel(Channel ch) {
    	for (Channel already : channels)
    		if (already == ch)
    			return;
    	
	    channels.add(ch);
	    addFader(ch);
    }
	    
    public void removeChannel(Channel ch) {
	    if (!channels.contains(ch)) 
		    return;
	    ChannelFader fade = getFader(ch);
	    remove(fade);
	    faders.remove(fade);
	    channels.remove(ch);
    }
 	    
    private void addFader(Channel ch) {
	    ChannelFader fader = new ChannelFader(ch);
	    faders.add(fader);
	    add(fader);
    }
    
	public void update(Channel channel) {
		for (ChannelFader ch : faders) 
			if (ch.getChannel().equals(channel)) 
				ch.update();
	}

	public void updateAll() {
		for (ChannelFader ch : faders) 
			ch.update();
	}

	public void highlight(Channel o) {
		for (ChannelFader ch : faders) 
			ch.setBorder(ch.getChannel() == o ? Constants.Gui.HIGHLIGHT : Constants.Gui.NO_BORDERS);
	}

	public ChannelFader getFader(Channel ch) {
		for (ChannelFader fade : faders)
			if (fade.getChannel() == ch)
				return fade;
		return null;
	}
}
