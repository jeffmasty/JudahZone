package net.judah.controllers;

import net.judah.api.Midi;

public interface Controller {
 	
	boolean midiProcessed(Midi midi);
	
}
