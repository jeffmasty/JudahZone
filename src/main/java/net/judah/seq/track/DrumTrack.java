package net.judah.seq.track;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

import lombok.Getter;
import net.judah.drumkit.DrumKit;
import net.judah.drumkit.DrumSample;
import net.judah.drumkit.DrumType;
import net.judah.drumkit.Drumz;
import net.judah.gui.TabZone;
import net.judah.midi.JudahMidi;
import net.judah.midi.Midi;
import net.judah.seq.Edit;
import net.judah.seq.Edit.Type;
import net.judah.seq.Meta;
import net.judah.seq.MidiPair;

public class DrumTrack extends NoteTrack {

	@Getter private final DrumKit kit;

	public DrumTrack(Drumz type, DrumKit kit) throws InvalidMidiDataException {
		super(type.name, kit.getActives());
		this.kit = kit;
		setCue(Cue.Hot);
		meta.setString(Meta.DEVICE, "BeatBox");
		meta.setInt(Meta.PORT, type.ordinal());
	}

	public DrumSample getSample(DrumType t) {
		return kit.getSamples()[t.ordinal()];
	}

	@Override protected void processNote(ShortMessage formatted) {
		midiOut.send(formatted, JudahMidi.ticker());
	}

	/** imports outside of 8 sample kit range */
	@Override protected void parse(Track incoming) {
		for (int i= 0; i < incoming.size(); i++) {
			MidiEvent e = incoming.get(i);
			if (Midi.isNoteOn(e.getMessage())) {
				ShortMessage orig = (ShortMessage)e.getMessage();
				int data1 = orig.getData1();
				t.add(new MidiEvent(Midi.create(
						orig.getCommand(), ch, data1, orig.getData2()), e.getTick()));
			}
			else if (e.getMessage() instanceof MetaMessage m)
				meta.incoming(m, e);
		}
	}

	@Override public boolean capture(Midi m) {
		if (!capture)
			return false;
		if (Midi.isNoteOn(m) && m.getChannel() >= DRUM_CH) {
			long tick = quantize(recent);
			TabZone.getDrummer(this).push(new Edit(Type.NEW,
					new MidiPair(Midi.createEvent(tick, NOTE_ON, ch, m.getData1(), m.getData2()), null)));
			if (tick < recent)
				midiOut.send(m, JudahMidi.ticker());
			return true;
		}
		return false;
	}

}
