package net.judah.mixer;

import java.awt.GridLayout;
import java.util.ArrayList;

import javax.swing.JPanel;

import lombok.Getter;
import net.judah.drumkit.DrumKit;
import net.judah.drumkit.DrumMachine;
import net.judah.drumkit.Sampler;
import net.judah.gui.Gui;
import net.judah.looper.Loop;
import net.judah.looper.Looper;

/** Graphical representation of the Mixer*/
public class DJJefe extends JPanel {

	@Getter private final ArrayList<Channel> all = new ArrayList<>();
	/** has GUI representation */
	@Getter private final ArrayList<Channel> channels = new ArrayList<>();
	private final ArrayList<ChannelFader> faders = new ArrayList<ChannelFader>();

    public DJJefe(Channel mains, Looper looper, Zone sources, DrumMachine drumMachine, Sampler sampler) {
        
    	all.addAll(looper);
		all.addAll(sources);
		for (DrumKit k : drumMachine.getKits())
			all.add(k);
		all.addAll(sampler);
		all.addAll(sampler.getStepSamples());
    	
    	for (Loop loop : looper) {
    		channels.add(loop);
    		ChannelFader fader = new LoopFader(loop, looper);
    		faders.add(fader);
    		add(fader);
    	}
        for (LineIn instrument : sources) {
        	channels.add(instrument);
    		ChannelFader fader = new LineInFader(instrument, looper.getSoloTrack());
    		faders.add(fader);
    		add(fader);
        }
        channels.add(mains);
        ChannelFader fader = new MainsFader(mains);
        faders.add(fader);
        add(fader);
        
    	setLayout(new GridLayout(1, channels.size()));
        doLayout();
    }

    
    public void addChannel(Channel ch) {
    	for (Channel already : channels)
    		if (already == ch)
    			return;
	    channels.add(ch);
    }
	    
    public void removeChannel(Channel ch) {
	    if (!channels.contains(ch)) 
		    return;
	    ChannelFader fade = getFader(ch);
	    remove(fade);
	    faders.remove(fade);
	    channels.remove(ch);
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

	public void highlight(ArrayList<Channel> s) {
		for (ChannelFader ch : faders) {
			ch.setBorder(s.contains(ch.getChannel()) ? Gui.HIGHLIGHT : Gui.NO_BORDERS);
		}
	}
	
	public void highlight(Channel o) {
		for (ChannelFader ch : faders) 
			ch.setBorder(ch.getChannel() == o ? Gui.HIGHLIGHT : Gui.NO_BORDERS);
	}

	public ChannelFader getFader(Channel ch) {
		for (ChannelFader fade : faders)
			if (fade.getChannel() == ch)
				return fade;
		return null;
	}
	
	public Channel byName(String channel) {
		for (Channel ch : all)
			if (ch.getName().equals(channel))
				return ch;
		return null;
	}

}
