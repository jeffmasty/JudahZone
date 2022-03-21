package net.judah.tracks;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.ShortMessage;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Data;

@Data 
public class Transpose {

	private int amount;
	
	public Transpose(int amount) {
		this.amount = amount;
	}
	
	public ShortMessage apply(ShortMessage midi, JackPort midiOut) throws InvalidMidiDataException {
		return new ShortMessage(midi.getCommand(), midi.getChannel(), midi.getData1() + amount, midi.getData2());
	}

}
