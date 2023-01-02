package net.judah.seq.beatbox;

import java.awt.Rectangle;

import net.judah.gui.Size;
import net.judah.seq.MidiConstants;

public interface BeatsSize extends MidiConstants {

	
	int WIDTH_PIANO = Size.TAB_SIZE.width - STEP_WIDTH;
	Rectangle BEATBOX_GRID = new Rectangle(1, MENU_HEIGHT, WIDTH_BEATBOX, GRID_HEIGHT / 2);

	Rectangle BEAT_STEPS = new Rectangle(1, MENU_HEIGHT + BEATBOX_GRID.height, WIDTH_BEATBOX, KEY_HEIGHT);
	Rectangle BOUNDS_MUTES = new Rectangle(BEATBOX_GRID.width + 1, BEATBOX_GRID.y, 2 * STEP_WIDTH + 10, BEATBOX_GRID.height);
	
	
	
}
