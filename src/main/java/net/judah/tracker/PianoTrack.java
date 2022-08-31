package net.judah.tracker;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import lombok.Setter;
import net.judah.midi.JudahClock;

public class PianoTrack extends Track {

	@Setter @Getter private int gate = 2;
	
	public PianoTrack(JudahClock clock, String name, int octave, JackPort port) {
		super(clock, name, 0, port);
		edit = new PianoEdit(this, octave);
	}
	
}