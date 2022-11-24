package net.judah.tracker;

import java.util.ArrayList;

import net.judah.api.Midi;
import net.judah.util.RTLogger;

public class Notes extends ArrayList<Midi> {
	
	public Notes(Midi... notes) {
		for (Midi msg : notes)
			add(msg);
	}
	
	public Notes(Notes notes) {
		for (Midi m : notes) {
			add(new Midi(m.getMessage()));
		}
	}

	@Override
	public void add(int index, Midi element) {
		if (element != null)
			super.add(index, element);
		else RTLogger.log(this, "wtf");
	}
	
	
	@Override
	public boolean add(Midi e) {
		if (e != null) 
			return super.add(e);
		RTLogger.log(this, "wtf");
		return false;
	}
	
	public Midi get() {
		if (isEmpty()) return null;
		return get(0);
	}
	
	public Midi find(int data1) {
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
