package net.judah.synth;

import javax.sound.midi.ShortMessage;

import net.judah.midi.Actives;
import net.judah.midi.Midi;

public class Polyphony extends Actives {
	
	final Voice[] voices;
	
	public Polyphony(JudahSynth out, int ch, int polyphony) {
		super(out, ch, polyphony);
		voices = new Voice[polyphony];
		for (int i = 0; i < voices.length; i++)
			voices[i] = new Voice(out);
	}
	
	/** only add under certain circumstances */
	@Override
	public boolean add(ShortMessage msg) {

		int data1 = msg.getData1();
		ShortMessage midi = find(data1);
		if (midi != null) {
			int idx = indexOf(midi);
			if (midi.getCommand() == ShortMessage.NOTE_OFF) 
				voices[idx].reset(data1); // re-press during release
			set(idx, Midi.copy(msg));
		}
		else if (getPolyphony() > size()) {
			super.add(Midi.copy(msg));
			voices[indexOf(data1)].reset(data1);
		}
		return true;
	}
	
	@Override
	public void noteOff(ShortMessage m) {
		for (int i = 0; i < size(); i++) {
			ShortMessage midi = get(i);
			if (midi != null && midi.getData1() == m.getData1()) {
				// start release on envelope,  when release completes note switched to null and Voice ready for polyphony
				set(i, Midi.create(Midi.NOTE_OFF, m.getChannel(), m.getData1(), midi.getData2()));
				return;
			}
		}
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

	@Override
	public boolean isEmpty() {
		for (ShortMessage m : this)
			if (m != null) return false;
		return true;
	}

	public void panic() {
		for (int i = size() - 1; i >= 0; i--) {
			ShortMessage m = remove(i);
			midiOut.send(Midi.create(ShortMessage.NOTE_OFF, m.getChannel(), m.getData1(), m.getData2()), 1);
		}
	}
	
}
