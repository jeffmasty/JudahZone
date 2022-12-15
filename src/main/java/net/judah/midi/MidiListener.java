package net.judah.midi;

public interface MidiListener {

	enum PassThrough {
		ALL, NONE, NOTES, NOT_NOTES
	}
	
	void feed(Midi midi);
	
	PassThrough getPassThroughMode();
	
}
