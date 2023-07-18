package net.judah.gui;

import java.awt.Dimension;

public interface Size {

	int WIDTH_FRAME = 1366;
    int WIDTH_KNOBS = 346;
    int WIDTH_TAB = WIDTH_FRAME - WIDTH_KNOBS - 2; 
    int HEIGHT_FRAME = 734;
    int HEIGHT_KNOBS = 260;
    int HEIGHT_MIXER = 150;
    int HEIGHT_TAB = HEIGHT_FRAME - HEIGHT_MIXER;

    Dimension TAB_SIZE = new Dimension(WIDTH_TAB, HEIGHT_TAB);
    Dimension KNOB_PANEL = new Dimension(WIDTH_KNOBS - 2, HEIGHT_KNOBS - 2);
    Dimension MIXER_SIZE = new Dimension(WIDTH_TAB, HEIGHT_MIXER);

    int STD_HEIGHT = 26;
    Dimension TITLE_SIZE = new Dimension(150, 33);
    Dimension WIDE_SIZE = new Dimension(120, STD_HEIGHT);
    Dimension COMBO_SIZE = new Dimension(100, STD_HEIGHT);
    Dimension MEDIUM_COMBO = new Dimension(82, STD_HEIGHT);
    Dimension SMALLER_COMBO = new Dimension(66, STD_HEIGHT);
    Dimension MICRO = new Dimension(51, STD_HEIGHT);
    Dimension TINY = new Dimension(43, STD_HEIGHT);
	Dimension MODE_SIZE = new Dimension(70, STD_HEIGHT);

}
