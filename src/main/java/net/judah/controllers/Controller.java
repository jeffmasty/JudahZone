package net.judah.controllers;

import org.jaudiolibs.jnajack.JackException;

import net.judah.midi.Midi;

public interface Controller {
 	
	boolean midiProcessed(Midi midi) throws JackException;
	
}
