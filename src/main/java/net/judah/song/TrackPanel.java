package net.judah.song;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

import net.judah.channel.Channel;
import net.judah.seq.TrackList;
import net.judah.seq.track.Computer;
import net.judah.seq.track.MidiTrack;

public class TrackPanel extends JPanel implements Iterable<SongTrack> {

	private final HashMap<MidiTrack, SongTrack> map = new HashMap<MidiTrack, SongTrack>();

	public TrackPanel() {
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
	}

	public Set<MidiTrack> getTracks() {
		return map.keySet();
	}

	@Override
	public Iterator<SongTrack> iterator() {
		return map.values().iterator();
	}

	public void refill(TrackList<MidiTrack> tracks) {
		removeAll();
		for (MidiTrack track : tracks) {
			if (map.containsKey(track) == false)
				map.put(track, new SongTrack(track));
			add(map.get(track));
		}
		invalidate();

		MidiTrack[] keys = map.keySet().toArray(new MidiTrack[map.size()]);
		for (MidiTrack key : keys)
			if (tracks.contains(key) == false)
				map.remove(key);
	}

	public void update(MidiTrack t) {
		SongTrack st = map.get(t);
		if (st != null && st.getCcTrack() != null)
			st.getCcTrack().repaint();
	}

	public SongTrack getTrack(Computer track) {
		return map.get(track);
	}

	public SongTrack getTrack(Channel ch) {
		for (SongTrack s : this)
			if (s.getTrack().getChannel() == ch)
				return s;
		return null;
	}

}
