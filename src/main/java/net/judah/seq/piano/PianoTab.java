package net.judah.seq.piano;

import java.util.ArrayList;

import net.judah.seq.MidiTab;
import net.judah.seq.MidiTrack;
import net.judah.seq.MidiView;
import net.judah.seq.TrackList;

public class PianoTab extends MidiTab {

	protected final ArrayList<MidiView> viewList = new ArrayList<>();
	
	public PianoTab(TrackList list) {
		super(list);
		for (MidiTrack t : tracks)
			viewList.add(new PianoView(t, this));
		changeTrack();
	}
	
	@Override
	public void changeTrack() {
		current = get(tracks.getCurrent());
		removeAll();
		add(current);
		setName(current.getTrack().getName());
		repaint();
	}

	MidiView get(MidiTrack t) {
		for (MidiView v : viewList)
			if (v.getTrack() == t)
				return v;
		return null;
	}



}
