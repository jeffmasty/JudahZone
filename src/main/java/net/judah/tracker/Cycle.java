package net.judah.tracker;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;

import lombok.Getter;
import lombok.Setter;

public class Cycle {
	
	@Setter @Getter private int count = 0; 
	private final Track track;
	
	public static final String[] CYCLES = new String[] {
			"[A][B]", "[AB][CD]", "[A3B][C3D]", "ABCD", /* "A3BC3D" */
	};
	
	public Cycle(Track t) {
		track = t;
	}

	public JComboBox<String> createComboBox() {
		JComboBox<String> result = new JComboBox<>();
		DefaultListCellRenderer center = new DefaultListCellRenderer(); 
		center.setHorizontalAlignment(DefaultListCellRenderer.CENTER); 
		result.setRenderer(center);
		for (String s : CYCLES)
			result.addItem(s);
		return result;
	}
	
	
	public Pattern cycle(int which) {
		int idx;
		switch (which) {
			case 0: // [A][B]
				return track.getCurrent();
			case 1: // [AB]
				count++;
				if (count % 2 == 0) { // back on even
					idx = track.indexOf(track.getCurrent()) - 1;
					if (idx < 0) 
						idx = track.size() - 1;
					if (track.size() > idx)
						return track.get(idx);
				}
				else { // progress on odd
					idx = 1 + track.indexOf(track.getCurrent());
					if (track.size() > idx)
						return track.get(idx);
				}
				break;
			case 2: // [A3B][C3D]
				count++;
				if (count == 4)
					count = 0;
				if (count < 3)
					return track.getCurrent();
				idx = 1 + track.indexOf(track.getCurrent());
				if (idx == track.size())
					idx = 0;
				if (track.size() > idx)
					return track.get(idx);
				break;
			case 3: // ABCD
				track.next(true);
				return track.getCurrent();
			// case 4: // A3BC3D // TODO
			default:
				throw new IllegalArgumentException("Unexpected value: " + which);
		}
		return track.getCurrent();
	}
}
