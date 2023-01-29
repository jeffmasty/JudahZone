package net.judah.gui.widgets;

import java.awt.event.ActionListener;

import javax.swing.JButton;

public class Btn extends JButton {
	
	public Btn(String lbl, ActionListener l) {
		super(lbl);
		addActionListener(l);
	}
}
