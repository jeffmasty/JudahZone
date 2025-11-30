package net.judah.seq.track;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;

import lombok.Getter;
import net.judah.api.ZoneMidi;
import net.judah.midi.Actives;
import net.judah.midi.Midi;
import net.judah.seq.automation.CC;
import net.judah.song.Sched;
import net.judah.util.Constants;
import net.judah.util.RTLogger;

@Getter
public abstract class NoteTrack extends MidiTrack {
    protected final Actives actives;
    /** parent midi port */
    protected final ZoneMidi midiOut;
    private float gain = 0.9f;

    public NoteTrack(String name, ZoneMidi out, int ch) throws InvalidMidiDataException {
    	super(name, ch);
		this.midiOut = out;
		actives = new Actives(midiOut, ch);
    }

    // DrumTrack
	public NoteTrack(String name, Actives actives) throws InvalidMidiDataException {
		super(name, actives.getChannel());
		this.actives = actives;
		this.midiOut = actives.getMidiOut();
	}

	/** publish the note/cc */
	@Override
	public void send(MidiMessage midi, long ticker) {
		if (midi instanceof MetaMessage) {
			// TODO
			return;
		}
		ShortMessage m = (ShortMessage)midi;
		if (Midi.isProgChange(midi)) {
			progChange(m.getData1());
			return;
		}
		if (CC.VOLUME.matches(m)) {// filter VOL CC
			setAmp(m.getData2() * Constants.TO_1);
			return;
		} else if (CC.STOP.matches(m)) {
			setActive(false);
			return;
		}

		if (m.getChannel() != ch) // conform to midi channel
			m = Midi.format(m, ch, 1);
		if (Midi.isNoteOn(m)) {
			try { // apply local gain
				m.setMessage(Midi.NOTE_ON, ch, m.getData1(), (int)(m.getData2() * gain));
			} catch (InvalidMidiDataException e) { RTLogger.warn(midi, e); }
		}
		midiOut.send(m, ticker);
	}

	public String getProgram() {
		return state.getProgram();
	}

	@Override public void setState(Sched sched) {
		if (sched.getProgram() != null && !sched.getProgram().equals(state.getProgram()))
			progChange(sched.getProgram());
		super.setState(sched);
	}

	@Override public String[] getPatches() {
		return midiOut.getPatches();
	}

	@Override public String progChange(int data1) {
		String name = midiOut.progChange(data1, ch);
		if (name == null)
			return null;
		state.setProgram(name);
		return name;
	}

	@Override public boolean progChange(String name) {
		String[] names = midiOut.getPatches();
		for (int i = 0; i < names.length; i++)
			if (names[i].equals(name))
				return progChange(i) != null;
		return false;
	}


}
