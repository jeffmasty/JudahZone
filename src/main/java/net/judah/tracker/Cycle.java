package net.judah.tracker;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;

import javax.swing.JComboBox;

import lombok.Getter;
import lombok.Setter;
import net.judah.songs.SmashHit;
import net.judah.tracker.Track.Cue;
import net.judah.util.CenteredCombo;

/** universal verse/trigger, cycle patterns for each track, provide/update comboBox widgets */
@Getter 
public class Cycle implements ActionListener {

	@Setter @Getter private static boolean verse; // vs. chorus
	@Getter private static boolean trigger;
	
	
	private int selected; 
	private int count;
	@Setter private boolean odd = false;
	@Setter private SmashHit custom;

	private final HashSet<JComboBox<String>> combos = new HashSet<>();
	
	public void setSelected(int i) {
		if (selected == i && custom == null)
			return;
		selected = i;
		count = 0;
		for (JComboBox<?> combo : combos) {
			combo.setSelectedIndex(selected);
		}
	}
	
	private final Track track;
	
	public static final String[] CYCLES = new String[] {
			"[A][B]", "[AB][CD]", "[A3B][C3D]", "ABCD"
	};
	
	public Cycle(Track t) {
		track = t;
	}

	public JComboBox<String> createComboBox() {
		JComboBox<String> result = new CenteredCombo<>();
		for (String s : CYCLES)
			result.addItem(s);
		result.addActionListener(this);
		combos.add(result);
		return result;
	}
	
	
	public void cycle() {
		if (custom != null) {
			custom.cycle(track);
			return;
		}
		
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
			default:
				throw new IllegalArgumentException("Unexpected cycle type: " + selected);
		}
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		int change = ((JComboBox<?>)e.getSource()).getSelectedIndex();
		if (selected != change)
			setSelected(change);
	}
	
	public static void setTrigger(boolean trig) {
		Cycle.trigger = trig;
		if (!trigger)
			return;
		for (Track t : Tracker.getTracks())
			if (t.getCue() == Cue.Trig)
				t.setCue(Cue.Bar);
	}
	
}
