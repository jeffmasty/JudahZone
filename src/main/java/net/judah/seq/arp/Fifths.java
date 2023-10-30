package net.judah.seq.arp;

import javax.sound.midi.ShortMessage;

import net.judah.seq.Poly;
import net.judah.seq.chords.Chord;

// stub
public class Fifths extends Algo {

	private final boolean init;
	
//	private int idx;
//	private int interval;
	private boolean ascending;

	public Fifths(boolean up) {
		ascending = init = up;
		
	}
	// C
	// 	  1 5 3 +1 5. 3 +1 5. 3 
	// C7
	//	  1 5 3 7 5 +1 7. 3 1 5 3 7. 5 +1 
	// C7b9
	//    1 5 3 7 5 +1 7. b9  (+)1 5....
	
	@Override
	public void process(ShortMessage m, Chord chord, Poly result) {
		if (chord == null) return;
		if (ascending) 
			up(m, chord, result);
		else 
			down(m, chord, result);
	}

	private void up(ShortMessage m, Chord chord, Poly result) {
	}	
	
	private void down(ShortMessage m, Chord chord, Poly result) {
	}
	
	@Override public void change() { // reset on chord change
		// interval = 0;
		ascending = init;
	}

	
}
