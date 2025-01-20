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
import net.judah.gui.Qwerty;
import net.judah.midi.JudahClock;
import net.judah.midi.JudahMidi;
import net.judah.midi.Midi;
import net.judah.seq.Edit;
import net.judah.seq.Edit.Type;
import net.judah.seq.MidiPair;
import net.judah.seq.Trax;
import net.judah.util.RTLogger;

public class DrumTrack extends MidiTrack {

	@Getter private final DrumKit kit;

	public DrumTrack(Trax type, DrumKit kit, JudahClock clock) throws InvalidMidiDataException {
		super(type, kit.getActives(), clock, kit);
		this.kit = kit;
	}

//	public DrumTrack(Engine out, Trax type, JudahClock clock) throws InvalidMidiDataException {
//		super(type.getName(), out, type.getCh())
//
//		super(kit.getKitMode().name(), kit, clock);
//		kit = new DrumKit(this, Trax.D1, "Pearl"),
//		this.kit = kit;
//	}


//	public DrumTrack(ZoneMidi engine, Trax type, JudahClock clock, String string) {
//		super(type.name(), engine clock);
//		this.kitMode = type;
//		this.
//		// TODO Auto-generated constructor stub
//		new DrumKit(this, Trax.D1, "Pearl"),
//	}

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
				ShortMessage orig = (ShortMessage)e.getMessage();
				int data1 = orig.getData1();
				if (DrumType.index(data1) >= 0)
					t.add(new MidiEvent(Midi.create(
							orig.getCommand(), ch, data1, orig.getData2()), e.getTick()));
				else if (DrumType.alt(data1) < 0) {
					int newVal = data1 % 6 + 2; // skipping bass and snare
					t.add(new MidiEvent(Midi.create(
							orig.getCommand(), ch, DrumType.values()[newVal].getData1(), orig.getData2()), e.getTick()));
					RTLogger.log(this, "remap'd " + data1 + " " + GMDrum.lookup(data1) + " to " + DrumType.values()[newVal]);
				}
				else {
					t.add(new MidiEvent(Midi.create(
							orig.getCommand(), ch, DrumType.alt(data1), orig.getData2()), e.getTick()));
				}
			}
		}
	}

	@Override
	public boolean capture(Midi m) {
		if (!capture)
			return false;
		if (Midi.isNoteOn(m) && m.getChannel() >= DRUM_CH) {
			long tick = quantize(recent);
			Qwerty.getDrummer(this).push(new Edit(Type.NEW,
					new MidiPair(Midi.createEvent(tick, NOTE_ON, ch, m.getData1(), m.getData2()), null)));
			if (tick < recent)
				midiOut.send(m, JudahMidi.ticker());
			return true;
		}
		return false;
	}


}
