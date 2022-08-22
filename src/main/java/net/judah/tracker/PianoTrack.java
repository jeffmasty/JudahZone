package net.judah.tracker;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import lombok.Setter;
import net.judah.midi.JudahClock;

public class PianoTrack extends Track {

	@Setter @Getter private int gate = 2;
	
	public PianoTrack(JudahClock clock, String name, int ch, JackPort port) {
		super(clock, name, ch, port);
		edit = new PianoEdit(this);
	}
	
}
