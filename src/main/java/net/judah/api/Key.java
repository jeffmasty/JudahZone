package net.judah.api;

import javax.sound.midi.ShortMessage;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor 
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

    @Getter private final int sharps;
    @Getter private final int flats;
    private final String alt;

    public static boolean isPlain(int data1) {
    	return Key.values()[data1 % 12].alt == null;
    }
    
    public Key key(ShortMessage m) {
		return Key.values()[m.getData1() % 12];
	}

    public String alt() { return alt; }
    
}
