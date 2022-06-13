package net.judah.tracks;

import java.util.ArrayList;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;

import lombok.Getter;
import lombok.Setter;
import net.judah.beatbox.Sequence;

public class Cycle extends JComboBox<String> {
	
	@Setter @Getter private int count = 0; 

	public static final String[] CYCLES = new String[] {
			"[A][B]", "[AB][CD]", "[A3B][C3D]", "ABCD", /* "A3BC3D" */
	};
	
	public Cycle() {
		for (String s : CYCLES)
			addItem(s);
		DefaultListCellRenderer center = new DefaultListCellRenderer(); 
		center.setHorizontalAlignment(DefaultListCellRenderer.CENTER); 
		setRenderer(center);
	}

	
	
	public ArrayList<Sequence> cycle(Box beats) {
		int idx;
		switch (getSelectedIndex()) {
			case 0: // [A][B]
				return beats.getCurrent();
			case 1: // [AB]
				count++;
				if (count % 2 == 0) { // back on even
					idx = beats.indexOf(beats.getCurrent()) - 1;
					if (idx < 0) 
						idx = beats.size() - 1;
					if (beats.size() > idx)
						return beats.get(idx);
				}
				else { // progress on odd
					idx = 1 + beats.indexOf(beats.getCurrent());
					if (beats.size() > idx)
						return beats.get(idx);
				}
				break;
			case 2: // [A3B][C3D]
				count++;
				if (count == 4)
					count = 0;
				if (count < 3)
					return beats.getCurrent();
				idx = 1 + beats.indexOf(beats.getCurrent());
				if (idx == beats.size())
					idx = 0;
				if (beats.size() > idx)
					return beats.get(idx);
				break;
			case 3: // ABCD
				return Box.next(true, beats, beats.getCurrent());
			// case 4: // A3BC3D // TODO
			default:
				throw new IllegalArgumentException("Unexpected value: " + getSelectedIndex());
		}
		return beats.getCurrent();
	}
}
