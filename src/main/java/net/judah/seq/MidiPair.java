package net.judah.seq;

import javax.sound.midi.MidiEvent;

import lombok.Data;

@Data
public class MidiPair {

	private final MidiEvent on;
	private final MidiEvent off;
	
}
