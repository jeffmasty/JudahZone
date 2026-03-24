package net.judah.seq.track;

import static net.judah.controllers.MPKTools.drumBank;
import static net.judah.controllers.MPKTools.drumIndex;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

import judahzone.api.DrumType;
import judahzone.api.Midi;
import lombok.Getter;
import net.judah.drums.Drumset;
import net.judah.midi.JudahMidi;
import net.judah.seq.track.Edit.Type;

public class DrumTrack extends NoteTrack {

	@Getter private final Drumset kit;

	public DrumTrack(String name, int ch, Drumset drums) {
		super(name, ch, drums, drums);
		this.kit = drums;
		cue = Cue.Hot;
		permanent = true; // for now
	}

	@Override
	public int[] getActiveSnapshot() {
		return kit.getActiveSnapshot();
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
		if (m.getChannel() < Midi.DRUM_CH) // translate drums??
			m = Midi.create(m.getCommand(), Midi.DRUM_CH + drumBank(m.getData1()),
				DrumType.values()[drumIndex(m.getData1())].getData1(), m.getData2());
		if (Midi.isNoteOn(m) && m.getChannel() >= Midi.DRUM_CH) {

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
