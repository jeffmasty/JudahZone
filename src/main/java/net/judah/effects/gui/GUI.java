package net.judah.effects.gui;

import java.awt.Dimension;

import net.judah.util.Constants.Gui;

public interface GUI {

    Dimension TAP_SZ = new Dimension(65, 22);
    Dimension TARGET_SZ = new Dimension(80, 25);
    Dimension MINI_LBL = new Dimension(40, 15);
    Dimension MINI = new Dimension(Gui.SLIDER_SZ.width - 5, Gui.SLIDER_SZ.height);
    Dimension SPACER = new Dimension(2, 1);
    
    int SHADE = 10;
    Dimension KNOB_LBL = new Dimension(40, 15);
    
}
