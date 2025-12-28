package net.judah.seq.track;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

import lombok.Getter;
import net.judah.api.Midi;
import net.judah.drumkit.DrumKit;
import net.judah.drumkit.DrumSample;
import net.judah.drumkit.DrumType;
import net.judah.drumkit.Drumz;
import net.judah.midi.JudahMidi;
import net.judah.seq.Edit;
import net.judah.seq.Edit.Type;

public class DrumTrack extends NoteTrack {

	@Getter private final DrumKit kit;

	public DrumTrack(Drumz type, DrumKit kit) throws InvalidMidiDataException {
		super(type.name, kit.getActives());
		this.kit = kit;
		cue = Cue.Hot;
	}

	@Override public DrumKit getChannel() {
		return kit;
	}

	public DrumSample getSample(DrumType t) {
		return kit.getSamples()[t.ordinal()];
	}

	@Override protected void processNote(ShortMessage formatted) {
		getMidiOut().send(formatted, JudahMidi.ticker());
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
		}
	}

	@Override public boolean capture(Midi m) {
		if (!capture)
			return false;
		if (Midi.isNoteOn(m) && m.getChannel() >= DRUM_CH) {
			long tick = quantize(recent);
			editor.push(new Edit(Type.NEW,
					Midi.createEvent(tick, NOTE_ON, ch, m.getData1(), m.getData2())));
			if (tick < recent)
				midiOut.send(m, JudahMidi.ticker());
			return true;
		}
		return false;
	}

}
