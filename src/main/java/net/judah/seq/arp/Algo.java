package net.judah.seq.arp;

import javax.sound.midi.ShortMessage;

import lombok.NoArgsConstructor;
import lombok.Setter;
import net.judah.seq.Poly;
import net.judah.seq.chords.Chord;

@NoArgsConstructor
public abstract class Algo {

	@Setter protected int range;

	public abstract void process(ShortMessage m, Chord chord, Poly result);
	
	/** if needed, subclasses can reset on chord changes */
	public void change() {
		
	}
	
}
