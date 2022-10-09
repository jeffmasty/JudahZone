package net.judah.tracker;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.api.MidiReceiver;
import net.judah.midi.Panic;
import net.judah.util.SettableCombo;

/** Connects sequencers and midi keyboards to either a Jack midi port or an internal midi consumer */
public class MidiCable extends SettableCombo<String> {
	
	@Getter private static final String[] portz = new String[] {"Synth1", "Synth2", "Crave", "Fluid1", "Fluid2", "Fluid3", "Fluid4"};
	
	private final TrackView view;
	
    public MidiCable(TrackView target, final JudahNotez switchboard) {
    	view = target;
    	for (String port : portz)
			addItem(port);
    	setSelected();
    	Runnable action = ()->{
    		Track t = view.getTrack();
    		new Panic(t.getMidiOut(), t.getCh()).start();
	    	t.setCh(getChannel("" + getSelectedItem()));
			t.setMidiOut(getReceiver("" + getSelectedItem()));
			view.getProgChange().reset(t);
	    };
    	setAction(action);
    }

    public static int getChannel(String port) {
		for (int i = 0; i < portz.length; i++) {
			if (portz[i].equals(port)) {
				if (i < 4)
					return 0;
				return i - 3;
			}
		}
		return 0;
	}

	public static MidiReceiver getReceiver(String port) {
		for (int i = 0; i < portz.length; i++)
			if (portz[i].equals(port)) {
				return i == 0 ? JudahZone.getSynth1() :
					i == 1 ? JudahZone.getSynth2() : 
					i == 2 ? JudahZone.getCrave() :
						JudahZone.getFluid();
			}
		return null;
	}
	

    public void setSelected() {
    	Track t = view.getTrack();
    	MidiReceiver out = t.getMidiOut();
		int ch = t.getCh();
		if (out == JudahZone.getSynth1())
			setSelectedIndex(0);
		else if (out == JudahZone.getSynth2())
			setSelectedIndex(1);
		else if (out == JudahZone.getCrave())
			setSelectedIndex(2);
		else 
			setSelectedIndex(ch + 3);
    }
    

    
}
