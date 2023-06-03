package net.judah.seq;

import java.util.ArrayList;
import java.util.List;

import net.judah.seq.chords.Key;

public class Poly extends ArrayList<Integer> {
	
	public boolean has(Key k) {
		for (Integer i : this) 
			if (Key.key(i) == k)
				return true;
		return false;
	}

	public Poly empty() {
		clear();
		return this;
	}

	public List<Integer> list() {
		return new ArrayList<Integer>(this);
	}
	
	public Integer[] array() {
		return toArray(new Integer[size()]);
	}
	
}
