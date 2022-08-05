package net.judah.tracker;

import java.util.ArrayList;

import net.judah.api.Midi;

public class Notes extends ArrayList<Midi> {
	
	public Notes() {
	}
	
	public Notes(Midi... notes) {
		for (Midi msg : notes)
			add(msg);
	}
	
	public Midi get() {
		if (isEmpty()) return null;
		return get(0);
	}
	
	Midi find(int data1) {
		for (Midi m : this)
			if (data1 == m.getData1())
				return m;
		return null;
	}

	public void erase(int data1) {
		for (Midi msg : new ArrayList<Midi>(this))
			if (data1 == msg.getData1())
				remove(msg);
	}
	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		for (Midi m : this) {
			sb.append(m.getData1()).append(" ");
		}
		
		return sb.toString();
	}
}
