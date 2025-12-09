package net.judah.gui;

import java.awt.Dimension;

public interface Size {

    int STD_HEIGHT = 26;
	int MENU_HEIGHT = Size.STD_HEIGHT + 6;
	int KNOB_HEIGHT = 32;
	int WIDTH_FRAME = 1368;
    int WIDTH_KNOBS = 346;
    int WIDTH_TAB = WIDTH_FRAME - WIDTH_KNOBS - 2;
    int HEIGHT_FRAME = 734;
    int HEIGHT_KNOBS = 290;
    int HEIGHT_MIXER = 154;
    int HEIGHT_TAB = HEIGHT_FRAME - HEIGHT_MIXER;

	Dimension SCREEN_SIZE = new Dimension(WIDTH_FRAME, HEIGHT_FRAME);
    Dimension TAB_SIZE = new Dimension(WIDTH_TAB, HEIGHT_TAB);
    Dimension KNOB_PANEL = new Dimension(WIDTH_KNOBS - 4, HEIGHT_KNOBS - 2);
    Dimension MIXER_SIZE = new Dimension(WIDTH_TAB, HEIGHT_MIXER);
    Dimension KNOB_TITLE = new Dimension(KNOB_PANEL.width, STD_HEIGHT + 2);

    Dimension TITLE_SIZE = new Dimension(150, 33);
    Dimension WIDE_SIZE = new Dimension(120, STD_HEIGHT);
    Dimension COMBO_SIZE = new Dimension(100, STD_HEIGHT);
    Dimension MEDIUM_COMBO = new Dimension(82, STD_HEIGHT);
	Dimension MODE_SIZE = new Dimension(70, STD_HEIGHT);
    Dimension SMALLER_COMBO = new Dimension(66, STD_HEIGHT);
    Dimension MICRO = new Dimension(49, STD_HEIGHT);
    Dimension TINY = new Dimension(41, STD_HEIGHT);
	Dimension FADER_SIZE = new Dimension(43, 76);

	int KEY_HEIGHT = 25;
	int STEP_WIDTH = 27;

}
