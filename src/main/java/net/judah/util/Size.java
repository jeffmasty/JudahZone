package net.judah.util;

import java.awt.Dimension;

public interface Size {

    int WIDTH_FRAME = 1365;
    int WIDTH_CONTROLS = 320;
    int WIDTH_SONG = WIDTH_FRAME - WIDTH_CONTROLS; // about 950
    int HEIGHT_MIXER = 180;
    int HEIGHT_FRAME = 744;
    int HEIGHT_TABS = HEIGHT_FRAME - HEIGHT_MIXER;
    
    Dimension TABS = new Dimension(WIDTH_SONG - 60, HEIGHT_TABS + 12);

    int STD_HEIGHT = 24;
    
    int WIDTH_CLOCK = 367;
    int WIDTH_KIT = 120;
    int WIDTH_BUTTONS = 150;
    int WIDTH_BBOX = 275;
    Dimension MICRO = new Dimension(45, 23);

    
}
