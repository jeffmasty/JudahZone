package net.judah.tracker;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.ShortMessage;

import lombok.Getter;
import lombok.Setter;
import net.judah.JudahZone;
import net.judah.util.RTLogger;

public class Transpose {

	@Getter private static boolean active;
	@Setter @Getter private static int amount;
	
	public static void setActive(boolean active) {
		Transpose.active = active;
		JudahZone.getMidiGui().transpose(active);
	}
	
	public static void toggle() {
		active = !active;
	}
	
	public static ShortMessage apply(ShortMessage midi)  {
		try {
			return new ShortMessage(midi.getCommand(), midi.getChannel(), midi.getData1() + amount, midi.getData2());
		} catch (InvalidMidiDataException e) {
			RTLogger.warn(Transpose.class, e);
		}
		return null;
	}

}
