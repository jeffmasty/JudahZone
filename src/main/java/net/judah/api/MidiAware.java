package net.judah.api;

import net.judah.midi.MidiPort;

public interface MidiAware {

	void setMidiPort(MidiPort port);
	MidiPort getMidiPort();
	
}
