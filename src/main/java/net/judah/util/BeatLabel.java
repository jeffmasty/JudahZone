package net.judah.util;

import java.awt.Color;

import javax.swing.JLabel;
import javax.swing.border.LineBorder;


public class BeatLabel extends JLabel {

    public BeatLabel(String s) {
        super(s);
        setAlignmentX(0.42f);
        setBorder(new LineBorder(Pastels.GREEN, 1, true));
        setBackground(Color.WHITE);
        setOpaque(true);
        setAlignmentX(0.5f);
    }

    public void setActive(boolean active) {
        if (active) {
            setBackground(Color.GREEN);
            setOpaque(true);
        }
        else setOpaque(false);
        repaint();
    }

}
