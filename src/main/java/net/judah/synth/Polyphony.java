package net.judah.synth;

import javax.sound.midi.ShortMessage;

import net.judah.gui.MainFrame;
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
	
	public void noteOn(ShortMessage msg) {

		int data1 = msg.getData1();
		
		ShortMessage midi = find(data1);
		
		if (midi != null) {
			int idx = indexOf(midi);
			if (midi.getCommand() == ShortMessage.NOTE_OFF) {
				// re-press during release
				voices[idx].reset(data1);
			}
			set(idx, Midi.copy(msg));
			return;
		}
		else if (getPolyphony() > size()) {
			add(Midi.copy(msg));
			voices[indexOf(data1)].reset(data1);
		}
		MainFrame.update(this);
	}
	
	@Override
	public void off(int data1) {
		noteOff(Midi.create(Midi.NOTE_OFF, data1, 1));
	}
	
	public void noteOff(ShortMessage m) {
		
		for (int i = 0; i < size(); i++) {
			ShortMessage midi = get(i);
			if (midi != null && midi.getData1() == m.getData1()) {
				// start release on envelope,  when release completes note switched to null and Voice ready for polyphony
				set(i, Midi.create(Midi.NOTE_OFF, m.getChannel(), m.getData1(), midi.getData2()));
				MainFrame.update(this);
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
		for (int i = 0; i < size(); i++) {
			ShortMessage m = get(i);
			midiOut.send(Midi.create(ShortMessage.NOTE_OFF, m.getChannel(), m.getData1(), m.getData2()), 1);
		}
	}
	
	
	@Override
	public void receive(ShortMessage msg) {
		if (Midi.isNoteOn(msg)) 
			noteOn(msg);
		else 
			noteOff(msg);
	}
}
