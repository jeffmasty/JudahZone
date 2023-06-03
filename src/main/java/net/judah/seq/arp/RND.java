package net.judah.seq.arp;

import java.util.Random;

import javax.sound.midi.ShortMessage;

import net.judah.seq.Poly;
import net.judah.seq.chords.Chord;
import net.judah.seq.chords.Key;

public class RND extends Algo implements Ignorant {

	private Random rnd = new Random();
	
	@Override
	public void process(ShortMessage m, Chord chord, Poly result) {
		result.add(m.getData1() 
				+ (Key.key(m.getData1()).up(chord.get(rnd.nextInt(chord.size())))) 
				+ 12 * rnd.nextInt(range / 12));
	}
	
}
