package net.judah.controllers;

import net.judah.api.Midi;
import net.judah.midi.JudahClock;


/** Handles input from a Behringer Crave USB connection */
public class Crave implements Controller {

	public static final String NAME = "CRAVE";
	
	@Override
	public boolean midiProcessed(Midi midi) {
		
		if (midi.getData1() == 12) { // let's mambo!
			float tempo = 0f;
			int dat = midi.getData2();
			if (dat < 32) 
				tempo = dat * 1.5f;
			else if (dat < 96) 
				tempo = dat + 17;
			else  {
				// 96 ~ 120
				tempo = 114 + 2 * (dat - 96);
			}
				
			JudahClock.getInstance().setTempo(tempo);
		}
		
		return true;  // don't sequence for now
	}

}
