package net.judah.midi;

import net.judah.api.TimeProvider;

public interface MidiClock extends TimeProvider {
	
	void processTime(byte[] midi);

}
