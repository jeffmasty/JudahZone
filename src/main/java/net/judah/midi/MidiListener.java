package net.judah.midi;

import net.judah.api.Midi;

public interface MidiListener {

	enum PassThrough {
		ALL, NONE, NOTES, NOT_NOTES
	}
	
	void feed(Midi midi);
	
	PassThrough getPassThroughMode();
	
}
