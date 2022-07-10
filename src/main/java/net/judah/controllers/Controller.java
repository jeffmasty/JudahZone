package net.judah.controllers;

import org.jaudiolibs.jnajack.JackException;

import net.judah.api.Midi;

public interface Controller {
 	
	boolean midiProcessed(Midi midi) throws JackException;
	
}
