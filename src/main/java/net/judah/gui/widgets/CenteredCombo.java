package net.judah.gui.widgets;

import java.awt.event.ActionListener;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

public class CenteredCombo <T> extends JComboBox<T> {

    public CenteredCombo() {
        ((JLabel)getRenderer()).setHorizontalAlignment(SwingConstants.CENTER);
    }

    public CenteredCombo(ActionListener l, T[] input) {
    	this();
    	addActionListener(l);
    }
    
}
