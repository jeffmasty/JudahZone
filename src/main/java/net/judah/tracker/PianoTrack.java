package net.judah.tracker;

import lombok.Getter;
import lombok.Setter;
import net.judah.api.MidiReceiver;

public class PianoTrack extends Track {

	@Setter @Getter private int gate = 2;
	
	public PianoTrack(String name, JudahNotez tracker, MidiReceiver out, int octave) {
		super(name, tracker, out, 0);
		edit = new PianoEdit(this, octave);
	}
	
	
	
}