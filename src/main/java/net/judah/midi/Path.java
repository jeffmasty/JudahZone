package net.judah.midi;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Data;
import net.judah.mixer.Channel;

@Data
public class Path {

	private final JackPort port;
	private final Channel channel;
	
}
