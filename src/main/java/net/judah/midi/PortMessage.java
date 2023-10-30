package net.judah.midi;

import javax.sound.midi.MidiMessage;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Data;

@Data
public class PortMessage {

	private final MidiMessage midi;
	private final JackPort port;
	
}
