package net.judah.mixer;

import static net.judah.JudahZone.*;

import java.awt.GridLayout;
import java.util.ArrayList;

import javax.swing.JPanel;

import lombok.Getter;
import net.judah.looper.Loop;
import net.judah.util.Constants;

/** Graphical representation of the Mixer*/
public class DJJefe extends JPanel {

	@Getter private final ArrayList<Channel> channels = new ArrayList<>();
	private final ArrayList<ChannelFader> faders = new ArrayList<ChannelFader>();
	
    public DJJefe() {

    	for (Loop loop : getLooper())
    		channels.add(loop);
    	
    	for (Loop loop : getLooper()) 
        	faders.add(loop.getFader());
    	
    	Channels instruments = getInstruments();
    	channels.add(instruments.getGuitar());
    	channels.add(instruments.getMic());
        channels.add(getSynth());
        channels.add(getBeats());
        channels.add(instruments.getCalf());
        channels.add(instruments.getFluid());
        channels.add(getSynth2());
        channels.add(getBeats2());
        channels.add(instruments.getCrave());
        channels.add(getMains());

        for (Channel ch : channels) {
        	ChannelFader fader = ch.getFader();
        	faders.add(fader);
        	add(fader);
        }
        setLayout(new GridLayout(1, faders.size()));
        doLayout();
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

	public Channel getChannel(String name) {
		for (Channel ch : channels)
			if (name.equals(ch.getName()))
				return ch;
		return null;
	}

	public ArrayList<GMSynth> getGMs() {
		ArrayList<GMSynth> result = new ArrayList<GMSynth>();
		for (Channel ch : channels) {
			if (ch instanceof GMSynth)
				result.add((GMSynth)ch);
		}
		return result;
	}
 	
}




/*  public static Channel getChannel(int idx) {
        switch(idx) {
            case 0: return JudahZone.getMains();
            case 1: return JudahZone.getLooper().getDrumTrack();
            case 2: return JudahZone.getLooper().getLoopA();
            case 3: return JudahZone.getLooper().getLoopB();
        } return JudahZone.getInstruments().get(idx - 4);}*/
