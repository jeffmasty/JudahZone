package net.judah.util;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

public class CenteredCombo <T> extends JComboBox<T> {

    public CenteredCombo() {
        ((JLabel)getRenderer()).setHorizontalAlignment(SwingConstants.CENTER);
    }

}
