package net.judah.seq.piano;

import java.awt.Rectangle;

import net.judah.gui.Size;
import net.judah.seq.MidiConstants;

public interface PianoSize extends MidiConstants {

	int WIDTH_PIANO = Size.TAB_SIZE.width - STEP_WIDTH;
	
	Rectangle BOUNDS_PIANIST = new Rectangle(STEP_WIDTH, MENU_HEIGHT, WIDTH_PIANO, KEY_HEIGHT);
	Rectangle BEAT_STEPS = new Rectangle(5, MENU_HEIGHT, WIDTH_BEATBOX, KEY_HEIGHT);
	Rectangle PIANO_GRID = new Rectangle(STEP_WIDTH, BOUNDS_PIANIST.height + BOUNDS_MENU.height, WIDTH_PIANO, GRID_HEIGHT);
	Rectangle PIANO_STEPS = new Rectangle(0, PIANO_GRID.y, STEP_WIDTH, GRID_HEIGHT);
	
	
	
}
