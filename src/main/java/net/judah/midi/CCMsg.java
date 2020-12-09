package net.judah.midi;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.ShortMessage;

import net.judah.api.Midi;

public class CCMsg extends Midi {

	/** create a Control_Change message for the preset instrument on the channel
	 * @throws InvalidMidiDataException */
	public CCMsg(int channel, int data1) throws InvalidMidiDataException {
		// first data1 bit not used (preset - 1)...
		super(ShortMessage.CONTROL_CHANGE, channel, data1, 0);
	}

	
}
