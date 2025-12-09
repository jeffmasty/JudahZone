package net.judah.seq.track;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.Sequence;
import javax.sound.midi.Track;

import net.judah.api.MidiClock;
import net.judah.seq.MidiTools;

public class MidiFile extends Sequence {

	public static int DEFAULT_RESOLUTION = MidiClock.MIDI_24;

	public static final int TYPE_1 = 1;

	public MidiFile() throws InvalidMidiDataException {
		this(MidiClock.MIDI_24);
	}

	public MidiFile(int resolution) throws InvalidMidiDataException {
		super(Sequence.PPQ, resolution, 0);
	}

	public MidiFile(Sequence sequence) throws InvalidMidiDataException {
		super(Sequence.PPQ, sequence.getResolution(), 0);
		for (Track t : sequence.getTracks()) {
			MidiTools.copy(t, createTrack());
		}
	}

	public void setResolution(int rez) {
		if (rez > 0)
			resolution = rez;
	}

}
