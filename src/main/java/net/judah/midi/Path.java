package net.judah.midi;

import lombok.Data;
import net.judah.mixer.Channel;

@Data
public class Path {

	private final MidiPort port;
	private final Channel channel;
	
}
