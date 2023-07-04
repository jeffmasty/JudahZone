package net.judah.seq.arp;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;

import net.judah.JudahZone;
import net.judah.gui.MainFrame;
import net.judah.midi.JudahMidi;
import net.judah.seq.Poly;
import net.judah.seq.chords.Chord;
import net.judah.seq.track.MidiTrack;

public class REC extends Algo implements Ignorant, Feed {

	private final MidiTrack track;
	
	public REC(MidiTrack track) {
		this.track = track;
   		JudahZone.getMidi().setKeyboardSynth(track);
	}
	
	@Override
	public void process(ShortMessage m, Chord chord, Poly result) {
		result.add(m.getData1()); // echo
	}

	@Override
	public void feed(ShortMessage midi) {
		// TODO Undoable
		long tick = track.quantize(track.getRecent());
		track.getT().add(new MidiEvent(midi, tick));
		if (tick <= track.getRecent()) 
			track.getMidiOut().send(midi, JudahMidi.ticker());
		MainFrame.update(track);
	}

	
}
