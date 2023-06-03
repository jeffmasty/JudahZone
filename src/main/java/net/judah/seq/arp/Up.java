package net.judah.seq.arp;

import javax.sound.midi.ShortMessage;

import lombok.RequiredArgsConstructor;
import net.judah.seq.Poly;
import net.judah.seq.chords.Chord;
import net.judah.seq.chords.Key;

@RequiredArgsConstructor
public class Up extends Algo {
	public static final int SIZE = 4;

	private int idx;
	private int interval = 0;
	
	@Override public void process(ShortMessage m, Chord chord, Poly result) {
		Key target = target(chord, idx++);
		if (target == null) {
			target = chord.getRoot();
			idx = 1;
		}
		int up = Key.key(m.getData1()).up(target);
		while (up < interval) 
			up += 12;
		interval = up >= range ? 0 : up;
		int data1 = m.getData1() + up;
		while (data1 > 127) 
			data1 -= 12;
		result.add(data1);
		idx %= SIZE;
	}

	public static Key target(Chord chord, int idx) {
		switch (idx++) {
			case 0: return chord.getRoot();
			case 1: return chord.get3rd(); 
			case 2: return chord.get5th(); 
			case 3: return chord.get7th(); 
		}
		return null;
	}
	
	@Override public void change() { // reset on chord changes
		interval = idx = 0;
	}
	

}
