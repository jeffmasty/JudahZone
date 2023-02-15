package net.judah.controllers;

import net.judah.midi.Midi;

public interface Controller {
 	
	/** @return true if consumed */
	boolean midiProcessed(Midi midi);
	
}
