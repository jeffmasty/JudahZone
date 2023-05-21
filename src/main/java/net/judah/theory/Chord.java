package net.judah.theory;

import java.util.ArrayList;

import lombok.Getter;
import lombok.Setter;
import net.judah.util.RTLogger;

@Getter @Setter
// under construction
public class Chord {
	
	// major, minor, maj7, dom7, aug, dim, root/isInversion, add9,11,13 b13,b9
	private String chord;
	private final ArrayList<Key> notes = new ArrayList<>();
	private Key root;
	
	public Chord(String parse) {
		int caret = 1;
		if (parse == null || parse.isEmpty()) {
			major("C");
			return;
		}
		String key = parse.substring(0, 1);
		if (parse.length() == 1) {
			major(key);
			return;
		}
		else {
			String second = parse.substring(1,2);
			if (second.equals("b")) {
				key += second;
				caret ++;
			}
			else if (second.equals("#")) {
				key += second;
				caret ++;
			}
		}

RTLogger.log(this, key + " key ... " + parse.substring(caret));
		
		if (caret == parse.length()) {
			major(key);
			return;
		}

		String suffix = parse.substring(caret);
		caret = 0;
		if (suffix.startsWith("m") || suffix.startsWith("-")) {
			minor(key);
			caret += 1;
		}
		else if (suffix.startsWith("dim")) {
			diminished(key);
			caret += 3;
		}
		else if (suffix.startsWith("aug")) {
			augmented(key);
			caret += 3;
		}
		else {
			major(key);
		}
		if (caret == suffix.length())
			return;
		if (suffix.substring(caret, caret + 1).equals("7")) {
			dominant7();
			caret++;
		}
		else if (suffix.length() > caret &&
				suffix.substring(caret).startsWith("maj7")) {
			major7();
			caret += 4;
		}
		
		if (suffix.length() > 3 + caret && suffix.substring(caret, caret + 3).equalsIgnoreCase("sus")) {
			sustain();
		}
		
		// TODO sus4, b9, b11 #13, add9, /root
	}
	
	private void major(String key) {
		root = Key.lookup(key);
		notes.add(root);
		notes.add(root.offset(4));
		notes.add(root.offset(7));
	}
	
	private void minor(String key) {
		root = Key.lookup(key);
		notes.add(root);
		notes.add(root.offset(3));
		notes.add(root.offset(7));
	}
	
	private void diminished(String key) {
		root = Key.lookup(key);
		notes.add(root);
		notes.add(root.offset(3));
		notes.add(root.offset(6));
	}
	
	private void augmented(String key) {
		root = Key.lookup(key);
		notes.add(root);
		notes.add(root.offset(4));
		notes.add(root.offset(8));
	}
	
	private void dominant7() {
		notes.add(root.offset(10));
	}
	
	private void major7() {
		notes.add(root.offset(11));
	}
	
	private void sustain() {
		if (notes.size() < 2)
			return;
		notes.set(1, notes.get(1).offset(1));
	}
	
	@Override
	public String toString() {
		
		Key alpha = notes.get(0);
		Key triad = notes.get(1);
		Key fifth = notes.get(2);
		
		StringBuffer buf = new StringBuffer("[");
		buf.append(alpha.name());
		if (alpha.compareTo(triad) == 3) {
			if (triad.compareTo(fifth) == 3)
				buf.append("dim");
			else
				buf.append("m");
		}
		else if (alpha.compareTo(fifth) == 8)
			buf.append("aug");
		else if (alpha.compareTo(fifth) == 6)
			buf.append("dim");
		if (notes.size() > 3) {
			Key seventh = notes.get(3);
			if (alpha.compareTo(seventh) == 10)
				buf.append("7");
			else if (alpha.compareTo(seventh) == 11)
				buf.append("maj7");
		}
		return buf.append("]").toString();
	}
	
}
