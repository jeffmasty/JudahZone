package net.judah.util;

import java.awt.Dimension;

public interface Size {

    int WIDTH_FRAME = 1282;
    int HEIGHT_FRAME = 742;
    int WIDTH_MIXER = 448;
    int WIDTH_SONG = WIDTH_FRAME - WIDTH_MIXER;
    int HEIGHT_CONSOLE = 140;
    int HEIGHT_TABS = HEIGHT_FRAME - HEIGHT_CONSOLE - 34;

    int STD_HEIGHT = 24;
    int WIDTH_CLOCK = 140;
    int WIDTH_TUNER = 210;
    Dimension CLOCK_SLIDER = new Dimension(90, STD_HEIGHT);
    Dimension MICRO = new Dimension(45, 23);


}
