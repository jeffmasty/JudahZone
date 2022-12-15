package net.judah.midi;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.ShortMessage;

import lombok.Getter;
import lombok.Setter;
import net.judah.JudahZone;
import net.judah.api.Notification.Property;
import net.judah.api.TimeListener;
import net.judah.util.RTLogger;

public class Transpose implements TimeListener {

	@Getter private static boolean active;
	@Setter @Getter private int amount;
	@Setter @Getter private static Integer onDeck;
	
	public Transpose(JudahClock clock) {
		clock.addListener(this);
	}
	
	public static void setActive(boolean active) {
		Transpose.active = active;
		JudahZone.getMidiGui().transpose(active);
	}
	
	public static void toggle() {
		setActive(!active);
	}

	public ShortMessage apply(ShortMessage midi)  {
		try {
			return new ShortMessage(midi.getCommand(), midi.getChannel(), midi.getData1() + amount, midi.getData2());
		} catch (InvalidMidiDataException e) {
			RTLogger.warn(Transpose.class, e);
		}
		return null;
	}

	@Override
	public void update(Property prop, Object value) {
		if (prop == Property.BEAT) {
			if (onDeck != null) { // shuffle to next transposition at bar downbeat
				amount = onDeck;
				onDeck = null;
			}
		}
	}

}
