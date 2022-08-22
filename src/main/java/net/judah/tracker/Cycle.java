package net.judah.tracker;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;

import lombok.Getter;
import lombok.Setter;
import net.judah.JudahZone;
import net.judah.util.RTLogger;

public class Cycle {
	
	@Setter @Getter private int count;
	@Setter @Getter private static boolean verse; // vs. chorus
	@Setter @Getter private static boolean trigger;
	@Setter private boolean odd = false;
	@Getter private int selected = 0; 
	
	public void setSelected(int i) {
		if (selected == i)
			return;
		selected = i;
		count = 0;
	}
	
	private final Track track;
	
	public static final String[] CYCLES = new String[] {
			"[A][B]", "[AB][CD]", "[A3B][C3D]", "ABCD", "Sleepwalk"/* "A3BC3D" */
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
		result.addActionListener(e -> selected = result.getSelectedIndex());
		return result;
	}
	
	
	public void cycle() {
		switch (selected) {
			case 0: // [A][B]
				return; 
			case 1: // [AB]
				track.next(++count % 2 == 0 ? false : true);
				return;
			case 2: // [A3B][C3D]
				count++;
				if (count == 4)
					track.next(true);
				if (count > 4) {
					track.next(false);
					count = 1;
				}
				return;
			case 3: // ABCD
				track.next(true);
				return;
			case 4: // Sleepwalk, bridge from 2 to 6 twice, then back to verse
				
				if (verse) {
					if (trigger) {
						trigger = false;
						if (count == 4) {
							track.setCurrent(track.get(0));
							verse = false;
							count = 0;
							odd = false;
							JudahZone.getLooper().getLoopA().setTapeCounter(0);
							JudahZone.getLooper().getLoopA().setOnMute(false);
							return;
						} 
						if (count == 0) {
							RTLogger.log(this, "Sleepwalk verse");
							JudahZone.getLooper().getLoopA().setOnMute(true);
							track.setCurrent(track.get(2));
						}
						else if (count == 1)
							track.setCurrent(track.get(3));
						else if (count == 2)
							track.setCurrent(track.get(2));
						else if (count == 3)
							track.setCurrent(track.get(4));
						
					}
					else {
						count++;
						trigger = true;
					}
					return;
				}
				else {
					track.next(!odd);
					odd = !odd;
				}					
				return;
			default:
				throw new IllegalArgumentException("Unexpected cycle type: " + selected);
		}
	}
}
