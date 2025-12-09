package net.judah.synth.taco;

import javax.sound.midi.ShortMessage;

import net.judah.api.Engine;
import net.judah.midi.Actives;
import net.judah.midi.Midi;

public class Polyphony extends Actives {

	/** max number of notes per channel */
	protected final int polyphony;
	public final Voice[] voices;

	public Polyphony(Engine out, int ch) {
		this(out, ch, TacoSynth.POLYPHONY);
	}

	public Polyphony(Engine out, int ch, int polyphony) {
		super(out, ch);
		this.polyphony = polyphony;
		voices = new Voice[polyphony];
	}

	/** only add under certain circumstances */
	@Override public boolean add(ShortMessage msg) {

		int data1 = msg.getData1();
		ShortMessage midi = find(data1);
		if (midi != null) {
			int idx = indexOf(midi);
			if (midi.getCommand() == ShortMessage.NOTE_OFF)
				voices[idx].reset(data1); // re-press during release
			set(idx, Midi.copy(msg));
		}
		else if (polyphony > size()) {
			super.add(Midi.copy(msg));
			voices[indexOf(data1)].reset(data1);
		}
		return true;
	}


	public int count() {
		int result = 0;
		for (ShortMessage m: this)
			if (m != null)
				result++;
		return result;
	}

	public ShortMessage highest() {
		int dat = -1;
		ShortMessage result = null;
		for (ShortMessage m : this) {
			if (m != null && m.getData1() > dat) {
				dat = m.getData1();
				result = m;
			}
		}
		return result;
	}

	public ShortMessage lowest() {
		int dat = 128;
		ShortMessage result = null;
		for (ShortMessage m : this) {
			if (m != null && m.getData1() < dat) {
				dat = m.getData1();
				result = m;
			}
		}
		return result;
	}

	@Override public boolean isEmpty() {
		for (ShortMessage m : this)
			if (m != null) return false;
		return true;
	}

	@Override public void setPedal(boolean pressed) {
		pedal = pressed;
		if (pedal)
			return;
		for (ShortMessage m : sustained)
			noteOff(m);
		sustained.clear();
	}

	@Override
	protected void retrigger(ShortMessage msg) {
		voices[indexOf(msg.getData1())].reset(msg.getData1());
	}

	@Override protected boolean noteOff(ShortMessage m) {
		int target = m.getData1();
		for (int i = 0; i < size(); i++) {
			ShortMessage midi = get(i);
			if (midi != null && midi.getData1() == target) {
				if (pedal) {
					if (susOf(midi.getData1()) < 0)
						sustained.add(midi);
				}
				else // start release on envelope,  when release completes note switched to null and Voice ready for polyphony
					set(i, Midi.create(Midi.NOTE_OFF, m.getChannel(), target, midi.getData2()));
				return true;
			}
		}
		return false;
	}

}
