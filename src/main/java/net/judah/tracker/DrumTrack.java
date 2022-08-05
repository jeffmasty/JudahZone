package net.judah.tracker;

import org.jaudiolibs.jnajack.JackPort;

import net.judah.midi.JudahClock;

public class DrumTrack extends Track {

	public DrumTrack(JudahClock clock, String name, int ch, JackPort port) {
		super(clock, name, ch, port);
	}

}
