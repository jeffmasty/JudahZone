package net.judah.seq.arp;

import javax.sound.midi.ShortMessage;

import net.judah.api.Algo;
import net.judah.api.Chord;
import net.judah.seq.Poly;

public class Chopin extends Algo {

	@Override
	public void process(ShortMessage bass, Chord chord, Poly result) {
		// Key root = chord.getRoot();

		// caret = bass + root
		// poly.add(caret)
		// while chord.hasNext() {
		// increment caret minor third
		// poly.add(chord.above(caret))
		// }

	}

}
