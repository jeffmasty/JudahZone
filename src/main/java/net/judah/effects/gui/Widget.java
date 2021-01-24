package net.judah.effects.gui;

import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import net.judah.mixer.Channel;
import net.judah.util.Constants.Gui;

public abstract class Widget extends JPanel implements GUI {

    protected final Channel channel;
    protected final JToggleButton activeButton;

    Widget(Channel channel, JToggleButton activeButton, ActionListener action) {
        this.channel = channel;
        setBorder(Gui.GRAY1);
        this.activeButton = activeButton;
        activeButton.addActionListener(action);
        activeButton.setAlignmentX(JLabel.CENTER_ALIGNMENT);

        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        add(Box.createRigidArea(EffectsRack.SPACER));
        add(activeButton);
        add(Box.createHorizontalGlue());

    }


    abstract void update();

    protected class Label extends JLabel {
        Label(String s) {
            super(" " + s + " ");
            setPreferredSize(MINI_LBL);
            setMaximumSize(MINI_LBL);
            setFont(Gui.FONT11);
        }
    }

}
