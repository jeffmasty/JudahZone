package net.judah.seq.arp;

import javax.sound.midi.ShortMessage;

import lombok.Getter;
import lombok.Setter;
import net.judah.JudahZone;
import net.judah.api.Notification.Property;
import net.judah.api.TimeListener;
import net.judah.midi.Midi;
import net.judah.seq.Cue;
import net.judah.seq.MidiConstants;
import net.judah.seq.MidiTrack;
import net.judah.seq.Poly;
import net.judah.seq.chords.Chord;

public class MPKTranspose extends Algo implements TimeListener, Feed, Ignorant {

	private final MidiTrack track;
	@Getter private int amount;
	@Setter @Getter private static Integer onDeck;
	
	public MPKTranspose(MidiTrack t) {
		this.track = t;
		track.getClock().addListener(this);
		JudahZone.getMidi().setKeyboardSynth(track);
	}

	public ShortMessage apply(ShortMessage midi)  {
		return Midi.create(midi.getCommand(), midi.getChannel(), midi.getData1() + amount, midi.getData2());
	}

	@Override
	public void update(Property prop, Object value) {
		if (prop == Property.BARS) {
			if (onDeck != null) { // shuffle to next transposition at bar downbeat
				amount = onDeck;
				onDeck = null;
			}
		}
	}

	@Override
	public void feed(ShortMessage midi) {
		if (track.getCue() == Cue.Hot)
			amount = midi.getData1() - MidiConstants.MIDDLE_C;
		else
			onDeck = midi.getData1() - MidiConstants.MIDDLE_C;
	}

	@Override
	public void process(ShortMessage m, Chord chord, Poly result) {
		result.add(m.getData1() + amount);
	}
	
}
