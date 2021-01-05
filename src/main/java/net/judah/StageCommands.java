package net.judah;

import static net.judah.JudahZone.*;

import net.judah.api.Midi;
import net.judah.looper.Recorder;
import net.judah.mixer.Channel;
import net.judah.mixer.Plugin;
import net.judah.plugin.BeatBuddy;
import net.judah.plugin.MPK;
import net.judah.util.Console;

public class StageCommands {
	
	/** if the midi msg is a hard-coded mixer setting, run the setting and return false */ 
    public boolean midiProcessed(Midi midi) {
    	
    	if (midi.isCC()) {
    		int data1 = midi.getData1();
    		
    		for (Channel c : getChannels()) {
    			if (data1 == c.getDefaultCC()) {
    				c.setVolume(midi.getData2());
    				return true;
    			}
    		}
    		
    		if (data1 == MPK.KNOBS.get(0)) {// loop A volume knob
    			getLooper().get(0).setVolume(midi.getData2());
    			return true;
    		}
    		
    		if (data1 == MPK.KNOBS.get(1)) {// loop B volume knob
    			getLooper().get(1).setVolume(midi.getData2());
    			return true;
    		}
    		
    		// TODO tie with beat buddy volume
    		if (data1 == MPK.KNOBS.get(6)) {
    			getChannels().getDrums().setVolume(midi.getData2());
    			if (getLooper().getDrumtrack() != null) 
    				getLooper().getDrumtrack().setVolume(midi.getData2());
    			return true;
    			
    			
			}
    		
    		if (data1 == 31) {// record loop A cc pad
    			((Recorder)getLooper().get(0)).record(midi.getData2() > 0);
    			return true;
    		}
    		if (data1 == 32) {// record loop B cc pad
    			((Recorder)getLooper().get(1)).record(midi.getData2() > 0);
    			return true;
    		}
    		
    		// TODO drum track record, double loop b length
    		if (data1 == 33 ) {// CC pad 2: slave loop B
    			new Thread() {
    				@Override public void run() {
    					getLooper().slave(); }}.start();

    		}
    		if (data1 == 34) { // clear loopers cc pad
    			getLooper().stopAll();
    			new Thread() {
    				@Override public void run() {
    					getLooper().init(); 
    				}
    			}.start();
    			return true;
    		}
    		if (data1 == 35) {// play loop A cc pad
    			getLooper().get(0).play(midi.getData2() > 0);
    			return true;
    		}
    		if (data1 == 36) {// play loop B cc pad
    			getLooper().get(1).play(midi.getData2() > 0);
    			return true;
    		}
    		
    		// play beat buddy
    		if (data1 == 37 && midi.getData2() > 0) {
    			getDrummachine().setOut(JudahZone.getMidi().getDrums());
    			getDrummachine().play();
    			return true;
    		}

    		
    		if (data1 == 38 && midi.getData2() > 0) { // setup a drumtrack slave loop
    			getLooper().drumtrack();
    			return true;
    		}

    		// foot pedal[0] octaver effect
    		if (data1 == MPK.PEDAL.get(0)) {
    			new Thread() {
    				@Override public void run() {
    	    			try { getCarla().octaver(midi.getData2() > 0);
    	    			} catch (Throwable t) { Console.warn(t);}
    				}}.start();
    		}

    		if (data1 == MPK.PEDAL.get(1)) { // mute Loop A foot pedal
    			getLooper().get(0).mute(midi.getData2() > 0);
    			return true;
    		}
    		if (data1 == MPK.PEDAL.get(2)) { // mute Loop B foot pedal
    			getLooper().get(1).mute(midi.getData2() > 0);
    			return true;
    		}
    		
    		if (data1 == MPK.PEDAL.get(3) && midi.getData2() > 0) { // let's hear some cymbols
    			getMidi().queue(BeatBuddy.CYMBOL_HIT);
    			return true;
    		}
    		if (data1 == MPK.PEDAL.get(4)) { // record Loop B foot pedal
    			((Recorder)getLooper().get(1)).record(midi.getData2() > 0);
    			return true;
    		}
    		if (data1 == MPK.PEDAL.get(5)) { // record Loop A foot pedal
    			((Recorder)getLooper().get(0)).record(midi.getData2() > 0);
    			return true;
    		}
    	} // end is CC
    	
    	else if (midi.isProgChange()) {
    		int data1 = midi.getData1();
    		
    		boolean result = false;
    		for (Plugin plugin : getPlugins()) 
    			if (plugin.getDefaultProgChange() == data1) {
    				plugin.activate(getChannels().getGuitar());
    				result = true;
    			}
    		if (result) return true;
    		
    		if (data1 == MPK.PRIMARY_PROG[3]) { // up instrument
    			new Thread() { @Override public void run() {
        			getSynth().instUp(0, true);    				
    			}}.start();
    			return true;
    		}
    		if (data1 == MPK.PRIMARY_PROG[7]) { // up instrument
    			new Thread() { @Override public void run() {
    				getSynth().instUp(0, false);
    			}}.start();
    			return true;
    		}

    	}
		return false;
	}

}
