package net.judah.controllers;

import static net.judah.JudahZone.getCarla;
import static net.judah.JudahZone.getDrummachine;
import static net.judah.JudahZone.getLooper;

import java.util.Arrays;
import java.util.List;

import net.judah.api.Midi;
import net.judah.util.Console;

public class MidiPedal implements Controller {
	
	public static final String NAME = "ActitioN MIDI Controller";
	
	// CC numbers sent
	public static final List<Integer> PEDAL = Arrays.asList(
			new Integer[] {96, 97, 98, 99, 100, 101});
	
	@Override
	public boolean midiProcessed(Midi midi) {
		int data1 = midi.getData1();
		
        if (data1 == PEDAL.get(0)) { // foot pedal[0] octaver effect

            new Thread() {
                @Override public void run() {
                    try { getCarla().octaver(midi.getData2() > 0);
                    } catch (Throwable t) { Console.warn(t);}
                }}.start();
        }

        if (data1 == PEDAL.get(1)) { // mute Loop A foot pedal
            getLooper().getLoopA().setOnMute(midi.getData2() > 0);
            return true;
        }
        if (data1 == PEDAL.get(2) && midi.getData2() > 0) { // trigger only foot pedal
        	if (getDrummachine().isPlay())
        		getDrummachine().transission(); // drummachine.send(BeatBuddy.CYMBOL, 100);
        	else if (getLooper().getLoopA().hasRecording())
        		getDrummachine().latchA();
        	else getDrummachine().listen(getLooper().getLoopA());
        	return true;
        }

        if (data1 == PEDAL.get(3) ) { // record Drum Track
            Console.info("Record Drum Track Toggle");
            getLooper().getDrumTrack().record(midi.getData2() > 0);
        }
        if (data1 == PEDAL.get(4)) { // latch Loop B
        	new Thread() { @Override public void run() {
                getLooper().syncLoop(getLooper().getLoopA(), getLooper().getLoopB()); }}.start();
            return true;
        }
        if (data1 == PEDAL.get(5)) { 
        	getLooper().stopAll(); // TODO toggle stop/play with list of loops that were stopped
            return true;
        }
		
		return false;
	}

}
