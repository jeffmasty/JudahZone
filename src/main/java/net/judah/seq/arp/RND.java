package net.judah.seq.arp;

import java.util.List;
import java.util.Random;

import javax.sound.midi.ShortMessage;

import net.judah.api.Algo;
import net.judah.api.Chord;
import net.judah.api.Key;

public class RND extends Algo implements Ignorant {

	private Random rnd = new Random();

	@Override
	public void process(ShortMessage m, Chord chord, List<Integer> result) {
		result.add(m.getData1()
				+ (Key.key(m.getData1()).up(chord.get(rnd.nextInt(chord.size()))))
				+ 12 * rnd.nextInt(range / 12));
	}

}
