package net.judah.effects.gui;

import java.awt.Dimension;
import java.awt.Font;

import javax.swing.JLabel;

import net.judah.util.Constants.Gui;

public class SizedLabel extends JLabel {

	public SizedLabel(String text, Dimension size, Font font) {
		super(text); // pad spaces?
	    setPreferredSize(size);
        setMaximumSize(size);	
	}
	
	public SizedLabel(String text, Dimension size) {
		this(text, size, Gui.FONT11);
	}
	
	
}
