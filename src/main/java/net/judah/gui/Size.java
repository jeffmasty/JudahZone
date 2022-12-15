package net.judah.gui;

import java.awt.Dimension;

public interface Size {

    int STD_HEIGHT = 24;
	int WIDTH_FRAME = 1366;
    int WIDTH_KNOBS = 348;
    int WIDTH_TAB = WIDTH_FRAME - WIDTH_KNOBS - 12; 
    int HEIGHT_FRAME = 748;
    int HEIGHT_KNOBS = 270;
    int HEIGHT_MIXER = 152;
    int HEIGHT_TAB = HEIGHT_FRAME - HEIGHT_MIXER;

    Dimension TAB_SIZE = new Dimension(WIDTH_TAB, HEIGHT_TAB);
    Dimension KNOB_PANEL = new Dimension(WIDTH_KNOBS - 2, HEIGHT_KNOBS - 2);
    Dimension MIXER_SIZE = new Dimension(WIDTH_FRAME - WIDTH_KNOBS, HEIGHT_MIXER);
    
    
    Dimension COMBO_SIZE = new Dimension(100, 28);
	Dimension SMALLER_COMBO = new Dimension(70, 28);
    Dimension MICRO = new Dimension(45, 23);
    
}
