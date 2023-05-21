package net.judah.seq;

import java.util.ArrayList;

import javax.swing.JComboBox;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.judah.JudahZone;
import net.judah.gui.MainFrame;
import net.judah.song.Cmd;
import net.judah.song.Cmdr;
import net.judah.song.Param;

@NoArgsConstructor
public class TrackList extends ArrayList<MidiTrack> implements Cmdr {

	private MidiTrack current;
	@Setter private Runnable update;
	@Getter private String[] keys;
	
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

	public TrackList(ArrayList<MidiTrack> mpk) {
		addAll(mpk);
		keys = new String[mpk.size()];
		for (int i = 0; i < keys.length; i++)
			keys[i] = mpk.get(i).getName();
	}

//	@Override
//	public int value(String key) {
//		for (int i = 0; i < size(); i++)
//			if (get(i).getName().equals(key))
//				return i;
//		return 0;
//	}

	@Override
	public String lookup(int value) {
		if (value >= 0 && value < size())
			return get(value).getName();
		return get(0).getName(); // fail
	}

	@Override
	public MidiTrack resolve(String key) {
		for (MidiTrack t : this)
			if (t.getName().equals(key))
				return t;
		return null;
	}

	@Override
	public void execute(Param p) {
		if (p.cmd == Cmd.MPK)
			JudahZone.getMidi().setKeyboardSynth(resolve(p.val));
	}

	
}
