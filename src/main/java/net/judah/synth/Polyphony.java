package net.judah.synth;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.judah.api.Midi;

@RequiredArgsConstructor
public class Polyphony {
	public static int MAX = 6;
	
	@Getter private final Midi[] notes = new Midi[MAX];
	private final Voice[] voices;
	
	public void noteOn(Midi m) {
		int data1 = m.getData1();
		for (int i = 0; i < MAX; i++) {
			if (notes[i] != null && notes[i].getData1() == data1) {
				notes[i] = Midi.copy(m);
				return;
			}
		}
		for (int i = 0; i < MAX; i++) 
			if (notes[i] == null) {
				notes[i] = Midi.copy(m);
				voices[i].reset(JudahSynth.midiToHz(m.getData1()));
				break;
		}
	}
	
	public void noteOff(Midi m) {
		for (int i = 0; i < MAX; i++)
			if (notes[i] != null && notes[i].getData1() == m.getData1()) {
				notes[i] = null;
				return;
			}
	}
	
	public int count() {
		int result = 0;
		for (Midi m: notes)
			if (m != null)
				result++;
		return result;
	}

	public int indexOf(int data1) {
		for (int i = 0; i < MAX; i++)
			if (notes[i] != null && notes[i].getData1() == data1)
				return i;
		return -1;
	}
	
	public Midi highest() {
		int dat = -1;
		Midi result = null;
		for (Midi m : notes) {
			if (m != null && m.getData1() > dat)
				result = m;
		}
		return result;
	}
	
	public Midi lowest() {
		int dat = 128;
		Midi result = null;
		for (Midi m : notes) {
			if (m != null && m.getData1() < dat)
				result = m;
		}
		return result;
	}
	
}
