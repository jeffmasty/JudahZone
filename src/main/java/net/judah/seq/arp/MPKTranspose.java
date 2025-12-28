package net.judah.seq.arp;

import javax.sound.midi.ShortMessage;

import lombok.Getter;
import lombok.Setter;
import net.judah.api.Notification.Property;
import net.judah.api.Algo;
import net.judah.api.Chord;
import net.judah.api.Midi;
import net.judah.api.TimeListener;
import net.judah.seq.MidiConstants;
import net.judah.seq.Poly;
import net.judah.seq.track.Cue;
import net.judah.seq.track.PianoTrack;

public class MPKTranspose extends Algo implements TimeListener, Feed, Ignorant {

	private final PianoTrack track;
	@Setter @Getter private static Integer onDeck;
	@Getter private int amount;

	public MPKTranspose(PianoTrack t) {
		this.track = t;
		track.getClock().addListener(this);
	}

	public ShortMessage apply(ShortMessage midi)  {
		return Midi.create(midi.getCommand(), midi.getChannel(), midi.getData1() + amount, midi.getData2());
	}

	@Override public void update(Property prop, Object value) {
		if (prop == Property.BARS) {
			if (onDeck != null) { // shuffle to next transposition at bar downbeat
				amount = onDeck;
				onDeck = null;
			}
		}
	}

	@Override public void feed(ShortMessage midi) {
		if (track.getCue() == Cue.Hot)
			amount = midi.getData1() - MidiConstants.MIDDLE_C;
		else
			onDeck = midi.getData1() - MidiConstants.MIDDLE_C;
	}

	@Override public void process(ShortMessage m, Chord chord, Poly result) {
		result.add(m.getData1() + amount);
	}

}
