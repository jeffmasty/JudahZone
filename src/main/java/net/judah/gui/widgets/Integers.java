package net.judah.gui.widgets;

import javax.swing.JComboBox;

public class Integers extends JComboBox<Integer> {
	public Integers(int start, int end) {
		for (int i = start; i < end; i++)
			addItem(i);
		setSelectedItem(0);
	}
}
