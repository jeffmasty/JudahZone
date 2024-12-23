package net.judah.seq.arp;

import javax.sound.midi.ShortMessage;

import lombok.NoArgsConstructor;
import lombok.Setter;
import net.judah.seq.Poly;
import net.judah.seq.chords.Chord;

@NoArgsConstructor
public abstract class Algo {

	@Setter protected int range;

	/**
	 * @param bass reference bass note/octave
	 * @param chord input chord to transform
	 * @param result transformation
	 */
	public abstract void process(ShortMessage bass, Chord chord, Poly result);
	
	/** if needed, subclasses can reset on chord changes */
	public void change() {
		
	}
	
}
