package net.judah.beatbox;

import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.JLabel;

public class BeatLabel extends JLabel {

    BeatLabel(String s) {
        super(s);
        setBorder(BorderFactory.createLineBorder(Color.GRAY));
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
