package net.judah.midi;

import org.jaudiolibs.jnajack.JackPort;

public abstract class ReRoute {

	public abstract void patch(JackPort out);
	
}
