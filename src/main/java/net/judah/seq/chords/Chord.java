package net.judah.seq.chords;

import static net.judah.seq.chords.Interval.*;

import java.util.ArrayList;

import javax.sound.midi.ShortMessage;

import lombok.Data;
import net.judah.api.Key;
import net.judah.seq.Poly;

@Data 
public class Chord extends ArrayList<Key> {
	
	// major, minor, maj7, dom7, aug, dim, root/isInversion, add9  TODO 11,13 b13,b9
	String chord;
	Key bass;
	int steps;
	String lyrics = "";
	
	public Chord(ShortMessage midi, int... intervals) {
		Key note = Key.key(midi);
		add(note);
		for (int x : intervals) {
			note = note.offset(x);
			add(note);
		}
	}

	public Chord(String parse, String lyrics, int length) {
		this(parse);
		this.steps = length;
		if (lyrics != null && !lyrics.equals("null"))
			this.lyrics = lyrics;
	}
	
	public Chord(String parse) {
		
		chord = parse;
		
		
		
		int caret = 1;
		if (parse == null || parse.isEmpty()) {
			major("C");
			return;
		}
		
		int inverted = parse.indexOf('/');
		if (inverted > 0 && parse.length() > inverted)
			bass = Key.lookup(parse.substring(inverted + 1));

		
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

		String suffix = parse.substring(caret);

		if (caret == parse.length()) {
			major(key);
			return;
		}

		caret = 0;
		if ( (suffix.startsWith("m") && suffix.startsWith("maj") == false)
				|| suffix.startsWith("-")) {
			minor(key);
			caret += 1;
		}
		else if (suffix.startsWith("dim")) {
			diminished(key);
			caret += 3;
		}
		else if (suffix.contains("aug")) {
			augmented(key);
			if (suffix.startsWith("aug"))
				caret += 3;
		}
		else {
			major(key);
		}
		
		if (caret == suffix.length())
			return;
		
		if (suffix.length() > 3 + caret && suffix.substring(caret, caret + 3).equalsIgnoreCase("sus")) 
			sustain();
		
		if (suffix.substring(caret, caret + 1).equals("7")) {
			dominant7();
			caret++;
		}
		else if (suffix.length() > caret &&
				suffix.substring(caret).startsWith("maj7")) {
			major7();
			caret += 4;
		} else if (suffix.contains("6"))
			sixth();

		if (suffix.contains("9")) {
			if (!isMaj7() && !isDominant())
				dominant7();
			if (suffix.contains("b9"))
				flat9();
			else if (suffix.contains("#9")) 
				sharp9();
			else
				add9();
		}
		
		// TODO b11 #13, 
	}

	public Key getRoot() { return isEmpty() ? null : get(0); }
	public Key getBass() { return bass == null ? getRoot() : bass; }
	public Key get3rd() { return size() > 1 ? get(1) : null; }
	public Key get5th() { return (size() > 2) ? get(2) : null; }
	public Key get7th() { return (size() > 3) ? get(3) : null; }
	public boolean isInversion() { return getBass() != getRoot(); }
	
	private void major(String key) {
		Key root = Key.lookup(key);
		add(root);
		add(root.offset(MAJOR.ordinal()));
		add(root.offset(FIFTH.ordinal()));
	}
	
	private void minor(String key) {
		Key root = Key.lookup(key);
		add(root);
		add(root.offset(MINOR.ordinal()));
		add(root.offset(FIFTH.ordinal()));
	}

	private void sustain() {
		if (size() < 2)
			return;
		set(1, get(1).offset(1));
	}
	
	private void diminished(String key) {
		Key root = Key.lookup(key);
		add(root);
		add(root.offset(MINOR.ordinal()));
		add(root.offset(DIMINISHED.ordinal()));
	}
	
	private void augmented(String key) {
		Key root = Key.lookup(key);
		add(root);
		add(root.offset(MAJOR.ordinal()));
		add(root.offset(AUGMENTED.ordinal()));
	}

	private void sixth() 	 	{ add(getRoot().offset(SIXTH.ordinal())); }
	private void dominant7() 	{ add(getRoot().offset(DOMINANT.ordinal())); }
	private void major7() 	 	{ add(getRoot().offset(MAJ7.ordinal())); }
	private void add9() 		{ add(getRoot().offset(WHOLE.ordinal())); }
	private void flat9() 		{ add(getRoot().offset(HALF.ordinal())); }
	private void sharp9() 		{ add(getRoot().offset(MINOR.ordinal())); }
	
	public void build() {
		if (isEmpty())
			return;
		StringBuffer buf = new StringBuffer();
		buf.append(getRoot());

		if(isMinor()) {
			if (isDiminished())
				buf.append("dim");
			else 
				buf.append("m");
			if (isAugmented())
				buf.append("#5");
		}
		else if (isAugmented())
			buf.append("aug");
		else if (isDiminished())
			buf.append("b5");
		
		if (isDominant())
			buf.append("7");
		else if (isMaj7())
			buf.append("maj7");
		if (isSus4()) 
			buf.append("sus4");
		if (isAdd9()) 
			buf.append("9");
		else if (isFlat9()) 
			buf.append("b9");
		
		if (isInversion()) 
			buf.append("/").append(bass.name());
		chord = buf.toString();
	}
	
	public String getChord() {
		if (chord == null)
			build();
		return chord;
	}
	
	@Override
	public String toString() {
		return "[" + getChord() + "]";
	}

	// also true on diminished
	public boolean isMinor() {
		if (size() < 2) return false;
		return get(0).up(get(1)) == MINOR.ordinal();
	}
	
	// also true on augmented
	public boolean isMajor() {
		if (size() < 2) return false;
		return get(0).interval(get(1)) == MAJOR.ordinal();
	}
	
	public boolean isSus4() {
		if (size() < 2) return false;
		return get(0).interval(get(1)) == FOURTH.ordinal();
	}
	
	public boolean isAugmented() {
		if (size() < 3) return false;
		return get(0).interval(get(2)) == AUGMENTED.ordinal();
	}
	
	public boolean isDiminished() {
		if (size() < 3) return false;
		return get(0).interval(get(2)) == DIMINISHED.ordinal();
	}
	
	public boolean isDominant() {
		if (size() < 4) return false;
		return get(0).up(get(3)) == DOMINANT.ordinal();
	}
	
	public boolean isMaj7() {
		if (size() < 4) return false;
		return get(0).up(get(3)) == MAJ7.ordinal();
	}
	
	public boolean isFlat9() {
		for (Key k : this)
			if (get(0).interval(k) == HALF.ordinal())
				return true;
		return false;
	}
	public boolean isAdd9() {
		for (Key k : this)
			if (get(0).interval(k) == WHOLE.ordinal())
				return true;
		return false;
	}
	
	public void tight(int low, Poly result) {
		outer: 
		for (int counter = low; counter < 127; counter++) {
			if (contains(Key.key(counter)) && result.has(Key.key(counter)) == false) {
				result.add(counter);
			}
			for (Key k : this) 
				if (result.has(k) == false) continue outer;
			return; // chord complete
		}
		return; // error
	}
	public void wide(int data1, Poly result) {
		boolean skip = true;
		outer: 
		for (int counter = data1; counter < 127; counter++) {
			if (contains(Key.key(counter)) && result.has(Key.key(counter)) == false) {
					if (!skip) 
						result.add(counter);
					skip = !skip;
			}
			for (Key k : this) 
				if (result.has(k) == false) continue outer;
			return; // chord complete
		}
		return; // error
		
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass())
			return false;
		Chord other = (Chord) obj;
		if (bass != other.bass) return false;
		if (size() != other.size()) return false;
		for (int i = 0; i < size();i++)
			if (get(i) != other.get(i))
				return false;
		return true;
	}

	@Override
	public int hashCode() {
		int prime = 37;
		int result = bass.ordinal() * prime;
		for (Key k : this)
			result += k.ordinal() * prime;
		return result;
	}
	
//	public static void test() {
//		Chord Cmaj7 = new Chord("Cmaj7");
//		Chord G7 = new Chord("G7");
//		Poly notes = new Poly(); 
//		Cmaj7.wide(36, notes);
//		StringBuffer s = new StringBuffer(Cmaj7.getChord()).append(": ");
//		for (int data1 : notes) s.append(Key.key(data1)).append(data1/12).append(" ");
//		RTLogger.log(Chord.class, s.toString());
//		s = new StringBuffer(G7.getChord()).append(": ");
//		G7.tight(36, notes);
//		for (int data1 : notes) s.append(Key.key(data1)).append(data1/12).append(" ");
//		RTLogger.log(Chord.class, s.toString());}
	
}
