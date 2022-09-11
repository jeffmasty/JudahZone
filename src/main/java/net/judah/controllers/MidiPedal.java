package net.judah.controllers;

import static net.judah.JudahZone.*;

import java.util.Arrays;
import java.util.List;

import net.judah.MainFrame;
import net.judah.api.Midi;
import net.judah.looper.Loop;
import net.judah.midi.JudahClock;
import net.judah.mixer.SoloTrack;
import net.judah.util.RTLogger;

public class MidiPedal implements Controller {
	
	public static final String NAME = "ActitioN MIDI Controller";
	private boolean octaver = false;
	
	// CC numbers sent
	public static final List<Integer> PEDAL = Arrays.asList(
			new Integer[] {96, 97, 98, 99, 100, 101});
	
	@Override
	public boolean midiProcessed(Midi midi) {
		int data1 = midi.getData1();
		int data2 = midi.getData2();
		
        if (data1 == PEDAL.get(0)) { // overdrive
        	getChannels().getGuitar().getOverdrive().setActive(data2 > 0);
        	MainFrame.update(getChannels().getGuitar());
        }
        if (data1 == PEDAL.get(1)) { // reverb
        	getChannels().getGuitar().getReverb().setActive(data2 > 0);
        	MainFrame.update(getChannels().getGuitar());
        }
        if (data1 == PEDAL.get(2) && midi.getData2() > 0) { // trigger only foot pedal
            new Thread() {@Override public void run() {try {
            		octaver = !octaver;
            		getCarla().octaver(octaver);
                } catch (Throwable t) { RTLogger.warn(this, t);}}}.start();
            return true;

//        	if (getDrummachine().isPlay())
//        		getDrummachine().transission(); // drummachine.send(BeatBuddy.CYMBOL, 100);
//        	else if (getLooper().getLoopA().hasRecording())
//        		getDrummachine().latchA();
//        	else getDrummachine().listen(getLooper().getLoopA());
        }

        if (data1 == PEDAL.get(3) ) {// latch Loop B
        	Loop b = getLooper().getLoopB();
        	b.setArmed(!b.isArmed());
        	MainFrame.update(b);
        	return true;
        }
        if (data1 == PEDAL.get(4)) { 
        	if (getLooper().getLoopA().hasRecording())
        		getLooper().pause(false);
        	else JudahClock.getInstance().togglePlay();
        		
        }
        if (data1 == PEDAL.get(5)) { 
        	// toggle Verse/Chorus sections by muting different tracks
        	for (Loop s : getLooper()) {
        		if (s.hasRecording() && s instanceof SoloTrack == false)
        			s.setOnMute(!s.isOnMute());
        	}
            return true;
        }
		
		return false;
	}

	
}
