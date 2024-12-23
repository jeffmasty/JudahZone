package net.judah.seq;

import java.util.ArrayList;
import java.util.Arrays;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;

import net.judah.midi.Midi;
import net.judah.seq.Edit.Type;
import net.judah.seq.track.MidiTrack;

public class Etudes {

	private final MusicBox midi;
	private final MidiTrack track;

	public Etudes(MidiTrack t, MusicBox player) throws InvalidMidiDataException {
		this.midi = player;
		this.track = t;

		final int ch = track.getCh();
		MidiMessage con = new Midi(Midi.NOTE_ON, ch, 36, 100);
		MidiMessage coff = new Midi(Midi.NOTE_OFF, ch, 36, 100);

		MidiMessage don = new Midi(Midi.NOTE_ON, ch, 37, 100);
		MidiMessage doff = new Midi(Midi.NOTE_OFF, ch, 37, 100);

		MidiEvent on1 = new MidiEvent(con, track.timecode(0, 4));
		MidiEvent off1 = new MidiEvent(coff, track.timecode(0, 8));
		MidiPair quarter = new MidiPair(on1, off1);

		MidiEvent on2 = new MidiEvent(don, track.timecode(0, 8));
		MidiEvent off2 = new MidiEvent(doff, track.timecode(0, 16));
		MidiPair half = new MidiPair(on2, off2);


		Edit add = new Edit(Type.NEW, Arrays.asList(half, quarter));

		midi.push(add);
		midi.selectFrame();

		Edit drag = new Edit(Type.TRANS, new ArrayList(midi.getSelected()));
		drag.setDestination(new Prototype(47, track.timecode(0, 0)));
		midi.push(drag);
	}

}
