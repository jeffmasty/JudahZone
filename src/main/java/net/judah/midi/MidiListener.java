package net.judah.midi;

public interface MidiListener {

	enum PassThrough {
		ALL, NONE, NOTES
	}
	
	void feed(Midi midi);
	
	PassThrough getPassThroughMode();
	
}
