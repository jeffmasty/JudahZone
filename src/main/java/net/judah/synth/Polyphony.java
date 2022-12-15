package net.judah.synth;

import static net.judah.synth.JudahSynth.POLYPHONY;

import javax.sound.midi.ShortMessage;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.judah.midi.Midi;
import net.judah.util.Constants;

@RequiredArgsConstructor
public class Polyphony {
	
	@Getter private final ShortMessage[] notes = new ShortMessage[POLYPHONY];
	private final Voice[] voices;
	private boolean monophonic;
	
	public boolean noteOn(ShortMessage m) {
		int data1 = m.getData1();
		if (monophonic)
			return monoOn(m);
		
		for (int i = 0; i < POLYPHONY; i++) {
			if (notes[i] != null && notes[i].getData1() == data1) {
				if (notes[i].getCommand() == ShortMessage.NOTE_OFF) {
					// re-press during release
					voices[i].reset(Constants.midiToHz(m.getData1()));
				}
				notes[i] = Midi.copy(m);
				return true;
			}
		}
		for (int i = 0; i < POLYPHONY; i++) 
			if (notes[i] == null) {
				notes[i] = Midi.copy(m);
				voices[i].reset(Constants.midiToHz(m.getData1()));
				return true;
		}
		return false;
	}
	
	boolean monoOn(ShortMessage m) {
		return false;
	}
	
	void monoOff(ShortMessage m) {
		;
	}
	
	public boolean noteOff(ShortMessage m) {
		//if (monophonic)
		//	return monoOff(m);
		
		for (int i = 0; i < POLYPHONY; i++)
			if (notes[i] != null && notes[i].getData1() == m.getData1()) {
				// start release on envelope,  when release completes note switched to null and Voice ready for polyphony
				notes[i] = Midi.create(m.getCommand(), m.getChannel(), m.getData1(), notes[i].getData2());
				return true;
			}
		return false;
	}
	
	public int count() {
		int result = 0;
		for (ShortMessage m: notes)
			if (m != null)
				result++;
		return result;
	}

	public int indexOf(int data1) {
		for (int i = 0; i < POLYPHONY; i++)
			if (notes[i] != null && notes[i].getData1() == data1)
				return i;
		return -1;
	}
	
	public ShortMessage highest() {
		int dat = -1;
		ShortMessage result = null;
		for (ShortMessage m : notes) {
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
		for (ShortMessage m : notes) {
			if (m != null && m.getData1() < dat) {
				dat = m.getData1();
				result = m;
			}
		}
		return result;
	}

	public boolean isEmpty() {
		for (ShortMessage m : notes)
			if (m != null) return false;
		return true;
	}

	public void panic() {
		for (int i = 0; i < POLYPHONY; i++) {
			ShortMessage m = notes[i];
			if (m != null && Midi.isNoteOn(m)) { // flip NoteOn to NoteOff for graceful release
				notes[i] = Midi.create(ShortMessage.NOTE_OFF, m.getChannel(), m.getData1(), m.getData2());
			}
		}
	}

	public void flush() {
		for (int i = 0; i < POLYPHONY; i++)
			notes[i] = null;
	}
	
}
