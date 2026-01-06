package net.judah.seq.arp;

import java.util.List;

import javax.sound.midi.ShortMessage;

import judahzone.api.Algo;
import judahzone.api.Chord;
import judahzone.api.Midi;
import judahzone.api.MidiConstants;
import judahzone.api.TimeListener;
import judahzone.api.Notification.Property;
import lombok.Getter;
import lombok.Setter;
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

	@Override public void process(ShortMessage m, Chord chord, List<Integer> result) {
		result.add(m.getData1() + amount);
	}

}
