package net.judah.midi;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.ShortMessage;

public class NoteOn extends Midi {

	/** middle C */
	public NoteOn() throws InvalidMidiDataException {
		super(ShortMessage.NOTE_ON, 0, MIDDLE_C);
	}
	
	/** Note on for the given note */
	public NoteOn(int channel, int note) throws InvalidMidiDataException {
		super(ShortMessage.NOTE_ON, channel, note, 127);
	}

}
