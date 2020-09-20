package net.judah.midi;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum JudahDevice {
	KEYBOARD("keyboard"), PEDAL("pedal"), MIDI_IN("midiIn");
	
	@Getter private final String portName;
}
