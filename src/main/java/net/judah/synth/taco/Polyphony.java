package net.judah.synth.taco;

import javax.sound.midi.ShortMessage;

import judahzone.api.Midi;
import judahzone.util.RTLogger;
import net.judah.gui.MainFrame;
import net.judah.midi.Actives;
import net.judah.synth.Engine;

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
	@Override
	public int add(ShortMessage msg) {
		int data1 = msg.getData1();
		int idx = indexOf(data1);
		if (idx < 0) {
			if (polyphony > size()) {
				idx = super.add(Midi.copy(msg));
				voices[idx].reset(data1);
				return idx;
			}
			else {
				RTLogger.log(this, "Polyphony limit reached, ignoring " + msg);
				return -1;
			}
		}
		else { // manipulate an existing note
			ShortMessage midi = active.get(idx);
			if (Midi.isNoteOff(midi))
				voices[idx].reset(data1); // re-press during release
			set(idx, Midi.copy(msg));
			return idx;
		}
	}

	public int count() {
		int result = 0;
		for (ShortMessage m: active)
			if (m != null)
				result++;
		return result;
	}

	public ShortMessage highest() {
		int dat = -1;
		ShortMessage result = null;
		for (ShortMessage m : active) {
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
		for (ShortMessage m : active) {
			if (m != null && m.getData1() < dat) {
				dat = m.getData1();
				result = m;
			}
		}
		return result;
	}

	@Override public boolean isEmpty() {
		for (ShortMessage m : active)
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
			ShortMessage midi = active.get(i);
			if (midi != null && midi.getData1() == target) {
				if (pedal) {
					if (susOf(midi.getData1()) < 0)
						sustained.add(midi);
				}
				else if (i < size()) // start release on envelope,
					// when release completes note switched to null and Voice ready for polyphony
					set(i, Midi.create(Midi.NOTE_OFF, m.getChannel(), target, midi.getData2()));
				MainFrame.update(this);
				return true;
			}
		}
		return false;
	}

}
