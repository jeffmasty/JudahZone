package net.judah.seq;

import java.util.Vector;

import javax.swing.JComboBox;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.gui.MainFrame;
import net.judah.seq.track.MidiTrack;
import net.judah.util.Constants;

/** MidiTrack bounded Vector that has a Current Track and increments through the GUI */
public class TrackList<T extends MidiTrack> extends Vector<T> {
	private static final long INTERVAL = 10 * Constants.GUI_REFRESH;

	private MidiTrack current;
	private long flooding;
	@Getter private String[] keys;

	public MidiTrack getCurrent() {
		if (current == null && !isEmpty())
			current = get(0);
		return current;
	}

	public void init (MidiTrack init) {
		current = init;
	}

	public void setCurrent(MidiTrack change) {
		if (JudahZone.isInitialized() && System.currentTimeMillis() < flooding)
			return;
		this.current = change;
		MainFrame.setFocus(this);
		flooding = System.currentTimeMillis() + INTERVAL;
	}

	public void next(boolean up) {
		int next = Constants.rotary(indexOf(current), size(), up);
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
