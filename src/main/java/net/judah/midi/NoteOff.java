package net.judah.midi;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.ShortMessage;

import net.judah.api.Midi;

public class NoteOff extends Midi {

	/** create a Note Off message for the preset instrument on the channel
	 * @throws InvalidMidiDataException */
	public NoteOff(int channel, int data1) throws InvalidMidiDataException {
		// first data1 bit not used (preset - 1)...
		super(ShortMessage.NOTE_OFF, channel, data1, 0);
	}
	
	
}
