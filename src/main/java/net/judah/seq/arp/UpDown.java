package net.judah.seq.arp;

import javax.sound.midi.ShortMessage;

import lombok.RequiredArgsConstructor;
import net.judah.seq.Poly;
import net.judah.seq.chords.Chord;
import net.judah.seq.chords.Key;

@RequiredArgsConstructor
public class UpDown extends Algo {
	public static final int SIZE = 4;
	private int idx;
	private int interval;
	private boolean descending;
	
	@Override
	public void process(ShortMessage m, Chord chord, Poly result) {
		if (descending) 
			down(m, chord, result);
		else 
			up(m, chord, result);
	}
	
	private void up(ShortMessage m, Chord chord, Poly result) {
		Key target = Up.target(chord, idx++);
		if (target == null) {
			target = chord.getRoot();
			idx = 1;
		}
		int up = Key.key(m.getData1()).up(target);
		while (up < interval) 
			up += 12;
		if (interval > range) {
			descending = true;
			interval = 0;
		}
		else interval = up;
		
		int data1 = m.getData1() + up;
		while (data1 > 127) 
			data1 -= 12;
		result.add(data1);
		idx %= SIZE;	
	}
	
	private void down(ShortMessage m, Chord chord, Poly result) {
		Key target = Down.target(chord, idx++);
		if (target == null) {
			target = chord.get5th() == null ? chord.getRoot() : chord.get5th();
			idx = 1;
		}
		int down = Key.key(m.getData1()).down(target);
		while (down < interval) 
			down += 12;
		if (down >= range) {
			descending = false;
			interval = 0;
		}
		else interval = down;
		
		int data1 = m.getData1() + range - down;
		while (data1 < m.getData1()) 
			data1 += 12;
		
		result.add(data1);
		idx %= SIZE;

	}
	
	@Override public void change() { // reset on chord change
		interval = 0;
		descending = false;
	}

}
