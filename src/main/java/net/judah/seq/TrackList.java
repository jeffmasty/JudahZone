package net.judah.seq;

import java.util.ArrayList;

import javax.swing.JComboBox;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.judah.gui.MainFrame;

@AllArgsConstructor @NoArgsConstructor
public class TrackList extends ArrayList<MidiTrack> {

	private MidiTrack current;
	@Setter private Runnable update;
	
	public MidiTrack getCurrent() {
		if (current == null && !isEmpty())
			current = get(0);
		return current;
	}
	
	public void setCurrent(MidiTrack change) {
		this.current = change;
		if (update != null)
			update.run();
		MainFrame.setFocus(this);
	}
	
	public void next(boolean up) {
		int next = indexOf(current) + (up ? 1 : -1);
		if (next >= size())
			next = 0;
		if (next < 0)
			next = size() - 1;
		setCurrent(get(next));
	}
	
	public JComboBox<MidiTrack> combo() {
		JComboBox<MidiTrack> result = new JComboBox<>(toArray(new MidiTrack[size()]));
		result.setSelectedItem(current);
		result.addActionListener(e->{
			setCurrent((MidiTrack)result.getSelectedItem());
		});
		return result;
	}

	
}
