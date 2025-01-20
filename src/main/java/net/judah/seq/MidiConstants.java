package net.judah.seq;

import java.util.List;

public interface MidiConstants {

	int NOTE_ON = 0x90;
	int NOTE_OFF = 0x80;
	int NAME_STATUS = 73;

	int VELOCITY = 99;
	List<Integer> BLACK_KEYS = List.of(1, 3, 6, 8, 10);
	int RATCHET = 1;
	int MIDDLE_C = 60;

	String FLAT = "\u266D";
	String SHARP = "\u266F";

	int KEY_HEIGHT = 25;
	int STEP_WIDTH = 27;
	int DRUM_CH = 9;

}
