package net.judah.sequencer;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.Sequence;

public class Seq2 {

	public static final int STD_MIDI = 24;
	public static final int SIXTEENTHS = 4;
	
	Seq2() throws InvalidMidiDataException {
		
		Sequence seq = new Sequence(Sequence.PPQ, STD_MIDI);
	}
	
	// read midi
	// play midi
	// respond to change (tempo)
	// record and save midi
	
}
