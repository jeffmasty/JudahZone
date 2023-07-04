package net.judah.seq.arp;


import javax.sound.midi.ShortMessage;

import net.judah.api.Key;
import net.judah.seq.Poly;
import net.judah.seq.chords.Chord;

public class Ethereal extends Algo {
	
	@Override public void process(ShortMessage m, Chord chord, Poly result) {
		Key root = chord.getRoot();
		Key fifth = chord.get5th();
		Key ninth = chord.isFlat9() ? root.next() : root.next().next();
		
		int data1;
		for (data1 = m.getData1(); data1< 127; data1++) 
			if (Key.key(data1) == root) {
				result.add(data1);
				break;
			}
		for (; data1 < 127; data1++) 
			if (Key.key(data1) == fifth) {
				result.add(data1);
				break;
			}
		for (; data1 < 127; data1++) 
			if (Key.key(data1) == ninth) {
				result.add(data1);
				return;
			}
		
		
	}
}


