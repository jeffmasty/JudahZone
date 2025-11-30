package net.judah.seq.track;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.Sequence;

import net.judah.api.MidiClock;

public class MidiFile extends Sequence {

	public static final int TYPE_1 = 1;

	public MidiFile() throws InvalidMidiDataException {
		this(MidiClock.MIDI_24);
	}

	public MidiFile(int resolution) throws InvalidMidiDataException {
		super(Sequence.PPQ, resolution, 0);
	}

	public void setResolution(int rez) {

		if (rez > 0)
			resolution = rez;
	}


	// Add Track (Name, Instrument(MidiOut) or Line(Auto-Only)
	// Remove Track
	// Rename Track
	// Copy
	//
	// 0 Mains
	// // // //
	// 1 Bass
	// 2 Taco
	// 3 Tk2
	// 4 F1
	// 5 F2
	// 6 F3

	// 7 L/T/F
	// 8 L/T/F
	// 9 D1		//
	// 10 D2	//
	// 11 H1	//
	// 12 H2	//
	// 13 L/T/F
	// 14 L/T/F
	// 15 L/T/F
	// Chords
}
