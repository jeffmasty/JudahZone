package net.judah.util;

import java.awt.Dimension;

public interface Size {

    int WIDTH_FRAME = 1350;
    int HEIGHT_FRAME = 744;
    int WIDTH_MIXER = 398;
    int WIDTH_SONG = WIDTH_FRAME - WIDTH_MIXER; // about 950
    int HEIGHT_MIXER = 195;
    int HEIGHT_TABS = HEIGHT_FRAME - HEIGHT_MIXER - 34;

    int STD_HEIGHT = 24;
    int WIDTH_CLOCK = 140;
    int WIDTH_KIT = 210;
    Dimension CLOCK_SLIDER = new Dimension(90, STD_HEIGHT);
    Dimension MICRO = new Dimension(45, 23);

}
