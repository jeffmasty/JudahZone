package net.judah.seq.arp;

import javax.sound.midi.ShortMessage;

import lombok.RequiredArgsConstructor;
import net.judah.seq.Poly;
import net.judah.seq.chords.Chord;
import net.judah.seq.chords.Key;

@RequiredArgsConstructor
public class Down extends Algo {
	public static final int SIZE = 4;

	private int idx;
	private int interval = 0;
	
	@Override
	public void process(ShortMessage m, Chord chord, Poly result) {

		Key target = target(chord, idx++);
		if (target == null) {
			target = chord.get5th() == null ? chord.getRoot() : chord.get5th();
			idx = 1;
		}

		int down = Key.key(m.getData1()).down(target);
		while (down < interval) 
			down += 12;
		interval = down >= range ? 0 : down;
		int data1 = m.getData1() + range - down;
		while (data1 < m.getData1()) 
			data1 += 12;
		
		result.add(data1);
		idx %= SIZE;
	}
	
	@Override public void change() { // latch to keyChange
		interval = idx = 0;
		
	}

	public static Key target(Chord chord, int idx) {
		switch (idx) {
			case 0: return chord.get5th();
			case 1: return chord.get3rd();
			case 2: return chord.getRoot();
			case 3: return chord.get7th();
		}
		
		return null;
	}

}
