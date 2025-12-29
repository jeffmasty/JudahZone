package net.judah.api;

import javax.sound.midi.ShortMessage;

public enum Key {

    C  (0, 0, null),
    Db (0, 5, "C#"),
    D  (2, 0, null),
    Eb (0, 3, "D#"),
    E  (4, 0, null),
    F  (0, 1, null),
    Gb (6, 6, "F#"),
    G  (1, 0, null),
    Ab (0, 4, "G#"),
    A  (3, 0, null),
    Bb (0, 2, "A#"),
    B  (0, 5, null);

	public static final String FLAT = "\u266D";
	public static final String SHARP = "\u266F";
	public static final int OCTAVE = 12;
	public static final float TUNING = 440;

	private Key(int sharps, int flats, String alt) {
		this.sharps = sharps; this.flats = flats; this.alt = alt;
	}
	private final int sharps;
    private final int flats;
    final String alt;

    public int getSharps() { return sharps; }
    public int getFlats() { return flats; }
    public String getAlt() { return alt; }

    public static boolean isPlain(int data1) {
    	return Key.values()[data1 % 12].alt == null;
    }

    public static Key lookup(String txt) {
    	for (Key k : values())
    		if (k.name().equals(txt) || txt.equals(k.alt))
    			return k;
    	return C; // fail
    }

    public static Key key(int data1) {
    	return Key.values()[data1 % OCTAVE];
    }

    public static Key key(ShortMessage m) {
    	return key(m.getData1());
	}

    public String alt() { return alt; }

	public Key offset(int offset) {
		int target = ordinal() + offset;
		while (target < 0)
			target += Key.values().length;
		target %= Key.values().length;
		return Key.values()[target];
	}

	public int interval(Key key) {
		int mine = ordinal();
		int other = key.ordinal();
		if (mine > other)
			mine -= OCTAVE;
		return other - mine;
	}

	public Key next() {
		if (ordinal() +  1 == values().length) return values()[0];
		return values()[ordinal() + 1];
	}
	public Key prev() {
		if (ordinal() == 0) return values()[values().length - 1];
		return values()[ordinal() - 1];
	}
	public int up(Key target) {
		if (this == target) return 0;
		int count = 1;
		Key temp = next();
		while (temp != target) {
			count++;
			temp = temp.next();
		}
		return count;
	}

	public int down(Key target) {
		if (this == target) return 0;
		int count = 1;
		Key temp = prev();
		while (temp != target) {
			count++;
			temp = temp.prev();
		}
		return count;
	}

	private static final int A4_POSITION = 9 + 4 * 12; // A4 is the 9th note in the 4th octave (0-indexed)

	public static float toFrequency(Note n) {
		return toFrequency(n.key(), n.octave());
	}

	public static float toFrequency(Key note, int octave) {
        int position = note.ordinal() + (octave + 1) * 12; // equal-tempered scale
        int semitones = position - A4_POSITION;
        return (float) (TUNING * Math.pow(2, semitones / 12.0));
    }

	/** @return the nearest Note for hz */
	public static Note toNote(float hz) {
	   Key nearestKey = null;
	    int nearestOctave = 0;
	    float minDifference = Float.MAX_VALUE; // absolute
	    float difference = Float.MAX_VALUE; // actual
	    // Iterate through all keys and octaves to find the closest match
	    for (int octave = 0; octave <= 8; octave++) { // Assuming the range of octaves is 0 to 8
	        for (Key key : Key.values()) {
	            float frequency = Key.toFrequency(key, octave);

	            float abs = Math.abs(frequency - hz);

	            if (abs < minDifference) {
	                minDifference = abs;
	                difference = frequency - hz;
	                nearestKey = key;
	                nearestOctave = octave;
	            }
	        }
	    }
	    return new Note(nearestKey, nearestOctave, -1 * difference);
	}

}



