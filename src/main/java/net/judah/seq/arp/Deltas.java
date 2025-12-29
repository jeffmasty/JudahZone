package net.judah.seq.arp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import javax.sound.midi.ShortMessage;


public class Deltas {

	private HashMap<ShortMessage, List<Integer>> deltas = new HashMap<>();

	public void add(ShortMessage on, List<Integer> work) {
		if (contains(on.getData1()))
			return; // re-trigger
		deltas.put(on, new ArrayList<Integer>(work)); // malloc
	}

	public Set<ShortMessage> keys() {
		return deltas.keySet();
	}

	public List<Integer> get(ShortMessage key) {
		return deltas.get(key);
	}

	public List<Integer> remove(ShortMessage off) {
		ShortMessage key = key(off.getData1());
		if (key == null)
			return null;
		return deltas.remove(key);
	}

	boolean contains(int data1) {
		for (ShortMessage m : deltas.keySet())
			if (m.getData1() == data1)
				return true;
		return false;
	}

	ShortMessage key(int data1) {
		for (ShortMessage m : deltas.keySet())
			if (m.getData1() == data1)
				return m;
		return null;
	}

	public Set<Entry<ShortMessage, List<Integer>>> list() {
		return new HashSet<Entry<ShortMessage, List<Integer>>>(deltas.entrySet());
	}

	public void clear() {
		deltas.clear();
	}

	public boolean isEmpty() {
		return deltas.isEmpty();
	}

}
