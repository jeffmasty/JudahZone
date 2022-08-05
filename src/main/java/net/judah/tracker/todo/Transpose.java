package net.judah.tracker.todo;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.ShortMessage;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import lombok.Setter;

public class Transpose {

	@Setter @Getter private static boolean active;
	@Setter @Getter private static int amount;
	
	public static void toggle() {
		active = !active;
	}
	
	public static ShortMessage apply(ShortMessage midi, JackPort midiOut) throws InvalidMidiDataException {
		return new ShortMessage(midi.getCommand(), midi.getChannel(), midi.getData1() + amount, midi.getData2());
	}

}
