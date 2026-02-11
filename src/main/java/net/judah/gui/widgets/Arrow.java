package net.judah.gui.widgets;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionListener;

import javax.swing.plaf.basic.BasicArrowButton;

import judahzone.gui.Pastels;

public class Arrow extends BasicArrowButton {
	public static final Dimension SIZE = new Dimension(23, 22);

	public Arrow(int dir, ActionListener action) {
		super(dir, Pastels.BG, Pastels.MY_GRAY, Color.DARK_GRAY, Color.LIGHT_GRAY);
		if (action != null)
			addActionListener(action);
		
	}
	
	@Override public Dimension getPreferredSize() { return SIZE; }
	@Override public Dimension getMaximumSize() { return SIZE; }
	@Override public Dimension getMinimumSize() { return SIZE; }

}
			