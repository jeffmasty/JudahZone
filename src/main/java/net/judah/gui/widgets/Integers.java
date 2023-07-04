package net.judah.gui.widgets;

import javax.swing.JComboBox;

public class Integers extends JComboBox<Integer> {
	public Integers(int start, int end) {
		for (int i = start; i < end; i++)
			addItem(i);
		setSelectedItem(0);
	}
	
	public static Integer[] generate(int start, int end) {
		int size = end - start;
		Integer[] result = new Integer[size];
		for (int i = 0; i < size; i++)
			result[i] = i + start;
		return result;
	}
}
