package net.judah.tracker;

import javax.sound.midi.ShortMessage;

import lombok.Getter;
import lombok.Setter;
import net.judah.api.MidiReceiver;
import net.judah.tracker.edit.PianoEdit;

public class PianoTrack extends Track {

	@Getter @Setter boolean latch;
	
	@Setter @Getter private int gate = 2;
	
	public PianoTrack(String name, SynthTracks tracker, MidiReceiver out, int octave) {
		super(name, tracker, out, 0);
		edit = new PianoEdit(this, octave);
	}

	public void process(ShortMessage msg, int ticker) {
		if (latch) 
			midiOut.send(((SynthTracks)tracker).getTranspose().apply(msg), ticker);
		else 
			midiOut.send(msg, ticker);

	}
	
	
	
}