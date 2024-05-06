package net.judah.seq.track;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

import lombok.Getter;
import net.judah.drumkit.DrumKit;
import net.judah.drumkit.DrumSample;
import net.judah.drumkit.DrumType;
import net.judah.drumkit.GMDrum;
import net.judah.gui.MainFrame;
import net.judah.midi.JudahClock;
import net.judah.midi.JudahMidi;
import net.judah.midi.Midi;
import net.judah.seq.Edit;
import net.judah.seq.Edit.Type;
import net.judah.seq.MidiPair;
import net.judah.util.RTLogger;

public class DrumTrack extends MidiTrack {
	
	@Getter private final DrumKit kit;
	
	public DrumTrack(DrumKit kit, JudahClock clock) throws InvalidMidiDataException {
		super(kit.getKitMode().name(), kit, clock);
		this.kit = kit;
	}
	
	@Override
	protected void playNote(ShortMessage formatted) {
		midiOut.send(formatted, JudahMidi.ticker());
	}

	@Override protected void flush() { /** no NoteOffs, no-op */ }

	public DrumSample getSample(DrumType t) {
		return kit.getSamples()[t.ordinal()];
	}
	
	/** imports outside of 8 sample kit range */
	@Override
	protected void parse(Track incoming) {
		for (int i= 0; i < incoming.size(); i++) {
			MidiEvent e = incoming.get(i);
			if (Midi.isNoteOn(e.getMessage())) {
				int data1 = ((ShortMessage)e.getMessage()).getData1();
				if (DrumType.index(data1) >= 0) 
					t.add(e);
				else if (DrumType.alt(data1) < 0) {
					int newVal = data1 % 6 + 2; // skipping bass and snare
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
			} 
		}
	}
	
	@Override
	protected void processRecord(ShortMessage m, long ticker) { 
		if (Midi.isNoteOn(m)) {
			long tick = quantize(recent);
			MainFrame.getMidiView(this).getGrid().push(new Edit(Type.NEW, 
					new MidiPair(Midi.createEvent(tick, NOTE_ON, ch, m.getData1(), m.getData2()), null)));
			if (tick < recent) 
				midiOut.send(m, ticker);
		}
	}

}
