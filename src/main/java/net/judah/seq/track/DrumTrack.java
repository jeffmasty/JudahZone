package net.judah.seq.track;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

import net.judah.api.MidiReceiver;
import net.judah.drumkit.DrumType;
import net.judah.drumkit.GMDrum;
import net.judah.midi.JudahClock;
import net.judah.midi.JudahMidi;
import net.judah.midi.Midi;
import net.judah.util.RTLogger;

public class DrumTrack extends MidiTrack {
	
	public DrumTrack(MidiReceiver kit, JudahClock clock) throws InvalidMidiDataException {
		super(kit.getName(), kit, DRUM_CH, JudahClock.MIDI_24, clock, DrumType.values().length);
	}

	@Override
	protected void playNote(ShortMessage formatted) {
		midiOut.send(formatted, JudahMidi.ticker());
	}

	@Override protected void flush() { /** no-op */ }

	/** imports outside of 8 sample kit range */
	@Override 
	public void importTrack(Track incoming, int rez) {
		super.importTrack(incoming, rez);
		for (int i = 0; i < incoming.size(); i++) {
			MidiEvent e = incoming.get(i);
			if (Midi.isNoteOn(e.getMessage())) {
				int data1 = ((ShortMessage)e.getMessage()).getData1();
				if (DrumType.index(data1) >= 0) 
					t.add(e);
				else if (DrumType.alt(data1) < 0) {
					int newVal = data1 % 6 + 2; // skip bass and snare
					ShortMessage orig = (ShortMessage)e.getMessage();
					t.add(new MidiEvent(Midi.create(
							orig.getCommand(), orig.getChannel(), DrumType.values()[newVal].getData1(), orig.getData2()), e.getTick()));
					RTLogger.log(this, "remap'd " + data1 + " " + GMDrum.lookup(data1) + " to " + DrumType.values()[newVal]);
				}
				else {
					ShortMessage orig = (ShortMessage)e.getMessage();
					t.add(new MidiEvent(Midi.create(
							orig.getCommand(), orig.getChannel(), DrumType.alt(data1), orig.getData2()), e.getTick()));
				}
			} // else t.add(e);
		}
	}
}
