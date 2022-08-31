package net.judah.sequencer;

import javax.sound.midi.ShortMessage;

public interface MidiQueue {

	public void queue(ShortMessage message);
	
}
