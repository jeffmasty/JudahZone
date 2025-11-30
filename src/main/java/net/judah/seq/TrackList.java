package net.judah.seq;

import java.util.Vector;

import javax.swing.JComboBox;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.gui.MainFrame;
import net.judah.seq.track.MidiTrack;
import net.judah.util.Constants;

/** MidiTrack bounded Vector that has a Current Track and increments on the GUI thread */
public class TrackList<T extends MidiTrack> extends Vector<T> {
	private static final long INTERVAL = 10 * Constants.GUI_REFRESH;

	@Getter final Clipboard clipboard = new Clipboard();
	private MidiTrack current;
	@Getter private String[] keys;
	private long flooding;

	public MidiTrack getCurrent() {
		if (current == null && !isEmpty())
			current = getFirst();
		return current;
	}

	public void setCurrent(MidiTrack change) {
		if (JudahZone.isInitialized() && System.currentTimeMillis() < flooding) // twirling a knob
			return;
		MidiTrack old = current;
		this.current = change;
		if (!JudahZone.isInitialized())
			return;
		MainFrame.setFocus(this);
		if (old != null)
			MainFrame.update(old);
		flooding = System.currentTimeMillis() + INTERVAL;
	}

	public T next(boolean up) {
		int next = Constants.rotary(indexOf(current), size(), up);
		T result = get(next);
		setCurrent(get(next));
		return result;
	}

	public JComboBox<MidiTrack> combo() {
		JComboBox<MidiTrack> result = new JComboBox<>(toArray(new MidiTrack[size()]));
		result.setSelectedItem(current);
		result.addActionListener(e->{
			setCurrent((MidiTrack)result.getSelectedItem());
		});
		return result;
	}

//	public T get(Trax id) {
//		for (T t : this)
//			if (t.getType() == id)
//				return t;
//		return null;
//	}

}
