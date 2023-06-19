package net.judah.seq;

import java.awt.Rectangle;
import java.util.List;

import net.judah.gui.Size;

public interface MidiConstants {

	int NOTE_ON = 0x90;
	int NOTE_OFF = 0x80;
	int NAME_STATUS = 73;
	int NOTE_OFFSET = 24;
	int VELOCITY = 99;
	List<Integer> BLACK_KEYS = List.of(1, 3, 6, 8, 10);
	public static final int RATCHET = 1;
	public static final int MIDDLE_C = 60;

	String FLAT = "\u266D";
	String SHARP = "\u266F";
	
	int KEY_WIDTH = 14;
	int KEY_HEIGHT = 24;
	int STEP_WIDTH = 27;
	int MENU_HEIGHT = Size.STD_HEIGHT + 10;
	int GRID_HEIGHT = Size.HEIGHT_TAB - MENU_HEIGHT - KEY_HEIGHT - 31;
	int WIDTH_BEATBOX = Size.TAB_SIZE.width - 2 * STEP_WIDTH - 15;
	Rectangle BOUNDS_MENU = new Rectangle(0, 0, Size.TAB_SIZE.width, MENU_HEIGHT);

	int MAX_FRAMES = 32; // 64 measures

}
