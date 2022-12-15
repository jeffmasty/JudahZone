package net.judah.seq;

import java.awt.Rectangle;

import net.judah.gui.Size;

public interface MidiSize extends MidiConstants {

	int KEY_WIDTH = 13;
	int KEY_HEIGHT = 26;
	int STEP_WIDTH = 53;
	
	int WIDTH_PNL = (Size.TAB_SIZE.width) - STEP_WIDTH;
	int HEADER_HEIGHT = Size.STD_HEIGHT + 11;
	int GRID_HEIGHT = Size.HEIGHT_TAB - HEADER_HEIGHT - KEY_HEIGHT - 35;
	
	Rectangle BOUNDS_MENU = new Rectangle(STEP_WIDTH, 0, WIDTH_PNL, HEADER_HEIGHT);
	Rectangle BOUNDS_PIANIST = new Rectangle(STEP_WIDTH, HEADER_HEIGHT, WIDTH_PNL, KEY_HEIGHT);
	Rectangle BOUNDS_GRID = new Rectangle(STEP_WIDTH, BOUNDS_PIANIST.height + BOUNDS_MENU.height, 
				WIDTH_PNL + 1, GRID_HEIGHT);
	Rectangle BOUNDS_STEPS = new Rectangle(0, BOUNDS_GRID.y, STEP_WIDTH, GRID_HEIGHT);

}
