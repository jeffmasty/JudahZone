package net.judah.seq;

import java.util.List;

public interface MidiConstants {

	int NOTE_ON = 0x90;
    int NOTE_OFF = 0x80;
    int NAME_STATUS = 73;
	int NOTE_OFFSET = 24;
	int VELOCITY = 99;
	int RESOLUTION = 256;
	List<Integer> BLACK_KEYS = List.of(1, 3, 6, 8, 10);
	public static final String[] NOTE_NAMES = {"C", "C#", "D", "Db", "E", "F", "F#", "G", "Ab", "A", "Bb", "B"};

}
