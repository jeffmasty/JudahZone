package net.judah.seq.arp;

import java.util.List;

import javax.sound.midi.ShortMessage;

import judahzone.api.Algo;
import judahzone.api.Chord;

public class Chopin extends Algo {

	@Override
	public void process(ShortMessage bass, Chord chord, List<Integer> result) {
		// Key root = chord.getRoot();

		// caret = bass + root
		// poly.add(caret)
		// while chord.hasNext() {
		// increment caret minor third
		// poly.add(chord.above(caret))
		// }

	}

}
