package net.judah.gui.widgets;

import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.JButton;

public class Btn extends JButton {
	
	public Btn(Icon icon, ActionListener l) {
		super(icon);
		addActionListener(l);
	}

	public Btn(Icon icon, ActionListener l, String tip) {
		this(icon, l);
		setToolTipText(tip);
	}
	
	public Btn(String lbl, ActionListener l) {
		super(lbl);
		addActionListener(l);
	}

	public Btn(String string, ActionListener actionListener, String string2) {
		this(string, actionListener);
		setToolTipText(string2);
	}
}
