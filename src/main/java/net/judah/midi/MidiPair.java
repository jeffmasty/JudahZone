package net.judah.midi;

import javax.sound.midi.MidiMessage;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor 
public class MidiPair {

	private byte[] from;
	private byte[] to;
	
	@JsonIgnore private transient Midi fromMidi;
	@JsonIgnore private transient Midi toMidi;
	
	public MidiPair(byte[] from, byte[] to) {
		this.from = from;
		this.to = to;
	}

	public MidiPair(MidiMessage note1, MidiMessage note2) {
		this(note1.getMessage(), note2.getMessage());
	}

	public MidiPair(MidiPair toCopy) {
		this(toCopy.from, toCopy.to);
	}
	
	@Override
	public String toString() {
		return getFromMidi() + " -> " + getToMidi();
	}

	public Midi getFromMidi() {
		if (fromMidi == null)
			fromMidi = new Midi(from);
		return fromMidi;
	}
	
	public Midi getToMidi() {
		if (toMidi == null)
			toMidi = new Midi(to);
		return toMidi;
	}
	
}
