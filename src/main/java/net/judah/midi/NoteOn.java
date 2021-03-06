package net.judah.midi;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.ShortMessage;

import net.judah.api.Midi;

public class NoteOn extends Midi {

	/** middle C */
	public NoteOn() throws InvalidMidiDataException {
		super(ShortMessage.NOTE_ON, 0, 60);
	}
	
	/** Note on for the given note */
	public NoteOn(int channel, int note) throws InvalidMidiDataException {
		super(ShortMessage.NOTE_ON, channel, note, 0);
	}

}
