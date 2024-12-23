package net.judah.seq.arp;

import javax.sound.midi.ShortMessage;

import net.judah.api.Key;
import net.judah.seq.Poly;
import net.judah.seq.chords.Chord;

public class Chopin extends Algo {

	@Override
	public void process(ShortMessage bass, Chord chord, Poly result) {
		Key root = chord.getRoot();
		
		// caret = bass + root
		// poly.add(caret)
		// while chord.hasNext() { 
		// increment caret minor third
		// poly.add(chord.above(caret))
		// }
		
	}

}
