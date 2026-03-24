package net.judah.seq.track;


import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;

import judahzone.api.ControlChange;
import judahzone.api.Midi;
import judahzone.api.MidiOut;
import judahzone.util.Constants;
import judahzone.util.RTLogger;
import lombok.Getter;
import lombok.Setter;
import net.judah.channel.Channel;
import net.judah.gui.MainFrame;
import net.judah.gui.knobs.TrackKnobs;
import net.judah.song.Sched;

@Getter
public abstract class NoteTrack extends MidiTrack {
	protected static final int NOTE_OFF = ShortMessage.NOTE_OFF;
	protected static final int NOTE_ON = ShortMessage.NOTE_ON;

    /** parent midi port */
    @Setter protected MidiOut midiOut;
	private Gate gate = Gate.SIXTEENTH;
	@Setter protected TrackKnobs trackKnobs; // Seq manages

	/**
	 * @param name    track name (nice if it was unique)
	 * @param ch      midi channel (0-15)
	 * @param line    sometimes line and midiOut are the same.
	 * @param midiOut  sometimes line is a midiOut container
	 */
	public NoteTrack(String name, int ch, Channel line, MidiOut midiOut) {
		super(name, ch, line);

		if (midiOut != null)
			this.midiOut = midiOut;
		else if (line instanceof MidiOut midi)
			this.midiOut = midi;
		else {
			RTLogger.warn(this, new NullPointerException("No midiOut for " + name));
			this.midiOut = null;
		}
	}

	// final here to MidiOut?:
//	public abstract String[] getPatches();
//	public abstract String progChange(int data1);
//	public abstract boolean progChange(String name);
	@Override
	public final String[] getPatches() {
		return midiOut.getPatches();
	}

	@Override public String progChange(int data1) {
		String name = midiOut.progChange(data1);
		if (name == null)
			return null;
		progSuccess(name);
		return name;
	}

	@Override public boolean progChange(String name) {
		int result = midiOut.progChange(name);
		if (result < 0)
			return false;
		progSuccess(name);
		return true;
	}

	private void progSuccess(String name) {
		state.setProgram(name);
		MainFrame.updateTrack(Update.PROGRAM, this);
	}


	// forward to midiOut's version of Notes/BOOL8
	public abstract int[] getActiveSnapshot();

	/** publish the note/cc */
	@Override public void send(MidiMessage midi, long ticker) {
		if (midi instanceof MetaMessage) {
			// TODO
			return;
		}
		ShortMessage m = (ShortMessage)midi;
		if (Midi.isProgChange(midi)) {
			progChange(m.getData1());
			return;
		}
		if (ControlChange.VOLUME.matches(m)) {// filter VOL CC
			setAmp(m.getData2() * Constants.TO_1);
			return;
		} else if (ControlChange.STOP.matches(m)) {
			setActive(false);
			return;
		}

		if (m.getChannel() != ch) // conform to midi channel
			m = Midi.format(m, ch, 1);
		midiOut.send(m, ticker);
	}

	@Override public void setState(Sched sched) {
		if (sched.getProgram() != null && !sched.getProgram().equals(state.getProgram()))
			progChange(sched.getProgram());
		super.setState(sched);
	}

	public long quantize(long tick) {
		if (clock.getTimeSig().div == 3 && (gate == Gate.SIXTEENTH || gate == Gate.EIGHTH))
			return triplets(tick, resolution);

		switch(gate) {
		case SIXTEENTH: return tick - tick % (resolution / 4);
		case EIGHTH: return tick - tick % (resolution / 2);
		case QUARTER: return tick - tick % resolution;
		case HALF: return tick - tick % (2 * resolution);
		case WHOLE: return tick - tick % (4 * resolution);
		case MICRO: return resolution > 16 ?
				tick - tick % (resolution / 8) : tick - tick % (resolution / clock.getTimeSig().div);
		case FILE : // approx MIDI_24
		case NONE :
		default:
			return tick;
		}
	}

	public long quantizePlus(long tick) {
		boolean swing = clock.getTimeSig().div == 3;
		long result = switch (gate) {
			case SIXTEENTH -> quantize(tick) + (swing ? resolution / 6 : resolution / 4);
			case EIGHTH -> 	quantize(tick) + (swing ? resolution / 3 : resolution / 2);
			case QUARTER -> quantize(tick) + (resolution);
			case HALF -> 	quantize(tick) + (2 * resolution);
			case WHOLE -> 	quantize(tick) + (4 * resolution);
			case MICRO -> 	quantize(tick) + (resolution / 8);
			//case NONE: case FILE:  // :	return quantize(tick) + 1/*RATCHET*/;
			default -> 		tick;
		};
		return result == tick ? result : result - 1; // hanging chads
	}

	protected long triplets(long tick, int resolution) {
		if (gate == Gate.SIXTEENTH)
			return tick - tick % (resolution / 6);
		return tick - tick % (resolution / 3);
	}

	public void setGate(Gate gate2) {
		gate = gate2;
		MainFrame.updateTrack(Update.GATE, this);
	}

}
