package net.judah.api;

import javax.sound.midi.ShortMessage;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Data;

@Data
public class PortMessage {

	private final ShortMessage midi;
	private final JackPort port;
	
}
