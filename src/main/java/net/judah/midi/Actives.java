package net.judah.midi;

import java.util.ArrayList;
import java.util.Set;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.ShortMessage;

import judahzone.api.Midi;
import judahzone.util.RTLogger;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.judah.gui.MainFrame;
import net.judah.synth.ZoneMidi;

/** listens to and tracks note_on and note_off going out a Midi port */
@RequiredArgsConstructor @Getter
public class Actives extends ArrayList<ShortMessage> {

	protected final ZoneMidi midiOut;
	protected final int channel;
	protected boolean pedal;
	protected final ArrayList<ShortMessage> sustained = new ArrayList<ShortMessage>();

	/** @return true if Pedal provides a veto */
	public boolean receive(ShortMessage msg) {
		if (Midi.isNoteOn(msg)) {
			if (pedal && indexOf(msg.getData1()) >= 0) {// re-trigger
				retrigger(msg);
				return true;
			}
			return add(msg);
		}
		if (Midi.isNoteOff(msg))
			return noteOff(msg);
		return true;
	}

    protected void retrigger(ShortMessage msg) {
		((MidiInstrument)midiOut).write(Midi.create(
				Midi.NOTE_OFF, channel, msg.getData1(), msg.getData2()));

    }

	/**@param ref  fill ref with data1 midi values of current voices */
	public void data1(Set<Integer> ref) {
		ref.clear();
		for (int i = 0; i < size(); i++)
			if (get(i) != null)
				ref.add(get(i).getData1());
	}

	public ShortMessage find(int data1) {
		for (int i = 0; i < size(); i++)
			if (get(i) != null && get(i).getData1() == data1)
				return (get(i));
		return null;
	}

	@Override public void clear() {
		super.clear();
		MainFrame.update(this);
	}

	protected int susOf(int data1) {
		for (int i= 0; i < sustained.size(); i++)
			if (sustained.get(i).getData1() == data1)
				return i;
		return -1;
	}

	public int indexOf(int data1) {
		for (int i = 0; i < size(); i++)
			if (get(i) != null && get(i).getData1() == data1)
				return i;
		return -1;
	}

	// Polyphony class overrides for JudahSynth
	protected boolean noteOff(ShortMessage msg) {
		if (pedal) {
			if (susOf(msg.getData1()) < 0)
				sustained.add(Midi.create(Midi.NOTE_OFF, channel, msg.getData1(), msg.getData2()));
			return false;
		}
		int idx = indexOf(msg.getData1());
		if (idx > 0) {
			remove(idx);
			MainFrame.update(this);
		}
		return true;
	}

	/** engage or release the foot pedal (CC64) */
	public void setPedal(boolean pressed) {
		pedal = pressed;
		if (pedal) // engaged
			return;
		try {
		for (ShortMessage sus : sustained)
			JudahMidi.queue(new ShortMessage(ShortMessage.NOTE_OFF, channel, sus.getData1(), 0), ((MidiInstrument)midiOut).getMidiPort());
			sustained.clear();
		} catch (InvalidMidiDataException e) {RTLogger.warn(this, e);}
	}



}
