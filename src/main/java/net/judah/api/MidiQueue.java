package net.judah.api;

import javax.sound.midi.ShortMessage;

public interface MidiQueue {

	public void queue(ShortMessage message);
	
}
