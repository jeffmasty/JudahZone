package net.judah.gui.widgets;

import java.util.ArrayList;

import javax.swing.JComboBox;

import net.judah.gui.Gui;
import net.judah.gui.Size;
import net.judah.midi.JudahClock;


public class LengthCombo extends JComboBox<Integer> {

	public static final Integer[] LENGTHS = {1, 1, 2, 2, 2, 3, 4, 4, 4, 4, 5, 6, 6, 7, 8, 8, 8, 8, 9, 
			10, 10, 11, 12, 12, 12, 13, 14, 15, 16, 16, 16, 16, 17, 18, 19, 20, 20, 21, 22, 23, 24, 
			24, 25, 26, 27, 28, 29, 30, 31, 32, 32, 32, 33, 34, 35, 36, 36, 40, 42, 44, 48, 64};

	public static final ArrayList<Integer> NO_DUPS = new ArrayList<>();
	static {
		for (Integer i : LENGTHS) {
			if (NO_DUPS.indexOf(i) < 0) 
				NO_DUPS.add(i);
		}
	}
	
	public LengthCombo(JudahClock clock) {
		for (Integer i : NO_DUPS) 
			addItem(i);
		setSelectedItem(JudahClock.getLength());
		addActionListener(e -> {
			if (JudahClock.getLength() != (int)getSelectedItem()) {
				clock.setLength((int)getSelectedItem());
			}
		});
		Gui.resize(this, Size.MICRO);

	}

	
}
