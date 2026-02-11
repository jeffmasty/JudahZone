package net.judah.seq.track;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

import judahzone.api.Midi;
import lombok.Getter;
import net.judah.drums.DrumInit;
import net.judah.drums.DrumKit;
import net.judah.drums.KitDB;
import net.judah.drums.KitDB.KitSetup;
import net.judah.drums.oldschool.OldSchool;
import net.judah.drums.synth.DrumSynth;
import net.judah.gui.MainFrame;
import net.judah.midi.JudahMidi;
import net.judah.seq.track.Edit.Type;

public class DrumTrack extends NoteTrack {

	@Getter private final DrumKit kit;

	public DrumTrack(DrumInit type, DrumKit kit) throws InvalidMidiDataException {
		super(type.name, kit.getActives());
		this.kit = kit;
		cue = Cue.Hot;
	}

	@Override
	public void load(TrackInfo info) {
		// TODO change kit if necessary
		if (info.getKit() != null) {
			if (info.getKit().equals(DrumSynth.TOKEN)) {
				if (kit instanceof DrumSynth synth)
					return;
			}
			else if (kit instanceof OldSchool samples) {
				KitSetup setup = KitDB.get(info.getKit(), false);
				if (setup != null)
					samples.accept(setup);
			}
		}
		else {
			// error case?
		}
		super.load(info);
	}

	@Override public DrumKit getChannel() {
		return kit;
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

	@Override public String progChange(int data1) {
		String name = kit.progChange(data1);
		if (name == null)
			return null;
		progSuccess(name);
		return name;
	}

	@Override public boolean progChange(String name) {
		boolean result = kit.progChange(name);
		if (!result)
			return false;
		progSuccess(name);
		return result;
	}

	private void progSuccess(String name) {
		state.setProgram(name);
		MainFrame.updateTrack(Update.PROGRAM, this);
	}
	@Override public String[] getPatches() {
		return kit.getPatches(); // oldSchool vs synth
	}
}
