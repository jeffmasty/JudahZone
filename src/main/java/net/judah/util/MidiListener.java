package net.judah.util;

import net.judah.midi.Midi;

public interface MidiListener {

	enum PassThrough {
		ALL, NONE, NOTES, NOT_NOTES
	}
	
	void feed(Midi midi);
	
	PassThrough getPassThroughMode();
	
}
