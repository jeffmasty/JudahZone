package net.judah.midi;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.ShortMessage;

/** Program Change MIDI message */
public class ProgMsg extends Midi {

	/** create a Program_Change message for the preset instrument on the channel
	 * @throws InvalidMidiDataException */
	public ProgMsg(int channel, int preset) throws InvalidMidiDataException {
		// first data1 bit not used (preset - 1)...
		super(ShortMessage.PROGRAM_CHANGE, channel, preset - 1, 0);
	}

}
