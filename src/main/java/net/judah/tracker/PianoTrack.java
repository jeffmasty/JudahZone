package net.judah.tracker;

import lombok.Getter;
import lombok.Setter;
import net.judah.midi.JudahClock;
import net.judah.midi.MidiPort;

public class PianoTrack extends Track {

	@Setter @Getter private int gate = 2;
	
	public PianoTrack(JudahClock clock, String name, int octave, MidiPort port, JudahBeatz t) {
		super(clock, name, 0, port, t);
		edit = new PianoEdit(this, octave);
	}
	
	
	
}