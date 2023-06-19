package net.judah.seq.chords;

import java.util.ArrayList;

public class Preview extends ArrayList<Chord> {

	boolean middle;
	
	@Override
	public void clear() {
		super.clear();
		middle = false;
	}
	
	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer();
		for (Chord chord : this) {
			buf.append(chord);
			buf.append("  ");
		}
		return buf.toString();
	}
}
