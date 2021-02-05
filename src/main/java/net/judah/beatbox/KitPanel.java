package net.judah.beatbox;

import static net.judah.util.Size.*;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;

import javax.swing.JPanel;

public abstract class KitPanel extends JPanel {

    protected static final Dimension SLIDESZ = new Dimension(61, STD_HEIGHT);
    protected static final Dimension BTNSZ = new Dimension(38, STD_HEIGHT);
    protected static final Dimension NAMESZ = new Dimension(107, STD_HEIGHT);

    protected final JPanel firstRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));

    public KitPanel() {
        setLayout(new GridLayout(0, 1, 0, 0));
        add(firstRow);
    }

}
