package net.judah.controllers;

import net.judah.api.Midi;

public class CircuitTracks implements Controller {

	@Override
	public boolean midiProcessed(Midi midi) {
		return true;
	}

}
