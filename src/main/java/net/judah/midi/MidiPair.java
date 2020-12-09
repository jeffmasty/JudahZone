package net.judah.midi;

import java.util.Arrays;

import javax.sound.midi.MidiMessage;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;
import lombok.NoArgsConstructor;
import net.judah.api.Midi;
import net.judah.plugin.MPK;
import net.judah.song.Edits.Copyable;

@Data @NoArgsConstructor 
public class MidiPair implements Copyable {

	private byte[] from;
	private byte[] to;
	
	@JsonIgnore private transient Midi fromMidi;
	@JsonIgnore private transient Midi toMidi;
	
	public MidiPair(byte[] from, byte[] to) {
		

		this.from = Arrays.copyOf(from, from.length);;
		this.to = Arrays.copyOf(to, to.length);;
	}

	public MidiPair(MidiMessage note1, MidiMessage note2) {
		this(note1.getMessage(), note2.getMessage());
	}

	public MidiPair(MidiPair toCopy) {
		this(toCopy.from, toCopy.to);
	}
	
	@Override
	public String toString() {
		return MPK.format(getFromMidi()) + " --> " + GMDrums.format(getToMidi());
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
	
	@Override
	public MidiPair clone() throws CloneNotSupportedException {
		return new MidiPair(from, to);
	}
}
