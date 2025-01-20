package net.judah.gui;

import java.awt.event.ActionListener;

import javax.swing.JMenuItem;

public class Actionable extends JMenuItem {
	public Actionable(String lbl, ActionListener l) {
		super(lbl);
		addActionListener(l);
	}
	public Actionable(String lbl, ActionListener l, int mnemonic) {
		this(lbl, l);
		setMnemonic(mnemonic);
	}
}
