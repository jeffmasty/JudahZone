package net.judah.controllers;

import static net.judah.JudahZone.getCarla;
import static net.judah.JudahZone.getChannels;
import static net.judah.JudahZone.getLooper;

import java.util.Arrays;
import java.util.List;

import net.judah.MixerPane;
import net.judah.api.Midi;
import net.judah.looper.Sample;
import net.judah.mixer.DrumTrack;
import net.judah.util.Console;

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
        	
        }
        if (data1 == PEDAL.get(1)) { // reverb
        	getChannels().getGuitar().getReverb().setActive(data2 > 0);
        	MixerPane.getInstance().update();
        }
        if (data1 == PEDAL.get(2) && midi.getData2() > 0) { // trigger only foot pedal
            new Thread() {@Override public void run() {try {
            		octaver = !octaver;
            		getCarla().octaver(octaver);
                } catch (Throwable t) { Console.warn(t);}}}.start();
            return true;

//        	if (getDrummachine().isPlay())
//        		getDrummachine().transission(); // drummachine.send(BeatBuddy.CYMBOL, 100);
//        	else if (getLooper().getLoopA().hasRecording())
//        		getDrummachine().latchA();
//        	else getDrummachine().listen(getLooper().getLoopA());
        }

        if (data1 == PEDAL.get(3) ) {// latch Loop B
        	new Thread() { @Override public void run() {
                getLooper().syncLoop(getLooper().getLoopA(), getLooper().getLoopB()); }}.start();
            return true;

        }
        if (data1 == PEDAL.get(4)) { 
        	getLooper().pause(false);
        }
        if (data1 == PEDAL.get(5)) { 
        	// toggle Verse/Chorus sections by muting different tracks
        	for (Sample s : getLooper().toArray()) {
        		if (s.hasRecording() && s instanceof DrumTrack == false)
        			s.setOnMute(!s.isOnMute());
        	}
            return true;
        }
		
		return false;
	}

	
}
