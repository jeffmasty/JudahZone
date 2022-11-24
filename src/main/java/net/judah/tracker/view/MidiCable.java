package net.judah.tracker.view;

import static net.judah.controllers.KnobMode.*;

import java.util.ArrayList;
import java.util.Arrays;

import net.judah.JudahZone;
import net.judah.api.MidiReceiver;
import net.judah.drumz.DrumKit;
import net.judah.midi.Panic;
import net.judah.tracker.Sequencers;
import net.judah.tracker.Track;
import net.judah.util.SettableCombo;

/** Connects sequencers and midi keyboards to either a Jack midi port or an internal midi consumer */
public class MidiCable extends SettableCombo<String> {
	
	private final ArrayList<String> list = new ArrayList<>();
	private final Track track;
	
	public MidiCable(final TrackView target, final Sequencers switchboard) {
    	track = target.getTrack();
    	if (track.isDrums())
    		for (DrumKit k : JudahZone.getDrumMachine().getDrumkits())
    			list.add(k.getName());
    	else 
    		list.addAll(Arrays.asList(new String[] {Synth1.name(), Synth2.name(), 
    				"Crave", "Fluid1", "Fluid2", "Fluid3", "Fluid4"}));
    	
    	for (String port : list)
			addItem(port);
    	setSelected();
    	Runnable action = ()->{
    		new Panic(track.getMidiOut(), track.getCh()).start();
    		if (track.isSynth())
    			track.setCh(getChannel("" + getSelectedItem()));
			track.setMidiOut(getReceiver("" + getSelectedItem()));
			target.getProgChange().reset(track);
	    };
    	setAction(action);
    }

    public int getChannel(String port) {
    	if (track.isDrums()) return track.getCh();
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i).equals(port)) {
				if (i < 4)
					return 0;
				return i - 3;
			}
		}
		return 0;
	}

	public MidiReceiver getReceiver(String port) {
		for (int i = 0; i < list.size(); i++)
			if (list.get(i).equals(port)) {
				if (track.isDrums())
					return (JudahZone.getDrumMachine().getDrumkits()[i]);
				return i == 0 ? JudahZone.getSynth1() :
					i == 1 ? JudahZone.getSynth2() : 
					i == 2 ? JudahZone.getCrave() :
						JudahZone.getFluid();
			}
		return null;
	}

    public void setSelected() {
    	MidiReceiver out = track.getMidiOut();
		if (track.isDrums()) {
			setSelectedItem(out.getName());
			return;
		}
		if (out == JudahZone.getSynth1())
			setSelectedIndex(0);
		else if (out == JudahZone.getSynth2())
			setSelectedIndex(1);
		else if (out == JudahZone.getCrave())
			setSelectedIndex(2);
		else 
			setSelectedIndex(track.getCh() + 3);
    }
    
}
