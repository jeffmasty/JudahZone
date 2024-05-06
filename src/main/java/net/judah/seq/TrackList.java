package net.judah.seq;

import java.util.ArrayList;
import java.util.Vector;

import javax.swing.JComboBox;

import lombok.Getter;
import lombok.NoArgsConstructor;
import net.judah.JudahZone;
import net.judah.gui.MainFrame;
import net.judah.seq.track.MidiTrack;
import net.judah.seq.track.PianoTrack;
import net.judah.song.cmd.Cmd;
import net.judah.song.cmd.Cmdr;
import net.judah.song.cmd.Param;
import net.judah.util.Constants;

@NoArgsConstructor
public class TrackList extends Vector<MidiTrack> implements Cmdr {

	private MidiTrack current;
	private long flooding;
	private static final long INTERVAL = 10 * Constants.GUI_REFRESH;
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

	public TrackList(ArrayList<MidiTrack> mpk) {
		addAll(mpk);
		keys = new String[mpk.size()];
		for (int i = 0; i < keys.length; i++)
			keys[i] = mpk.get(i).getName();
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
			JudahZone.getMidi().getMpk().setMidiTrack((PianoTrack)resolve(p.val));
	}

	
}
