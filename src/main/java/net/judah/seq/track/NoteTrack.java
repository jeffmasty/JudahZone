package net.judah.seq.track;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;

import lombok.Getter;
import lombok.Setter;
import net.judah.api.Midi;
import net.judah.api.ZoneMidi;
import net.judah.gui.MainFrame;
import net.judah.gui.knobs.TrackKnobs;
import net.judah.midi.Actives;
import net.judah.seq.automation.ControlChange;
import net.judah.song.Sched;
import net.judah.util.Constants;

@Getter
public abstract class NoteTrack extends MidiTrack {
    protected final Actives actives;
    /** parent midi port */
    protected final ZoneMidi midiOut;
	private Gate gate = Gate.SIXTEENTH;
	@Setter protected TrackKnobs trackKnobs; // Seq manages

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

	@Override public String[] getPatches() {
		return midiOut.getPatches();
	}

	@Override public String progChange(int data1) {
		String name = midiOut.progChange(data1, ch);
		if (name == null)
			return null;
		state.setProgram(name);
		MainFrame.updateTrack(Update.PROGRAM, this);
		return name;
	}

	@Override public boolean progChange(String name) {
		String[] names = midiOut.getPatches();
		for (int i = 0; i < names.length; i++)
			if (names[i].equals(name))
				return progChange(i) != null;
		return false;
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
