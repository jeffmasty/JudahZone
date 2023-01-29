package net.judah.seq;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import lombok.Getter;
import net.judah.api.MidiReceiver;
import net.judah.gui.Size;
import net.judah.gui.knobs.KnobPanel;
import net.judah.gui.knobs.TrackKnobs;
import net.judah.song.Sched;

@Getter 
public class Seq implements Iterable<MidiTrack>, MidiConstants {
	public static final int TRACKS = 10;
	public static final String NAME = "Seq";
	
	Dimension size = new Dimension(Size.WIDTH_TAB / 3, (Size.HEIGHT_TAB - 50) / 5);

	private final TrackList tracks = new TrackList();
	private final TrackList drumTracks;
	private final TrackList synthTracks;
	private final ArrayList<TrackKnobs> knobs = new ArrayList<>();

	public Seq(TrackList drumTracks, TrackList synthTracks) {
		this.drumTracks = drumTracks;
		this.synthTracks = synthTracks;
		tracks.addAll(drumTracks);
		tracks.addAll(synthTracks);

		for(MidiTrack track : this) 
			knobs.add(new TrackKnobs(track, this));
		
		drumTracks.forEach(track->track.setCycle(CYCLE.AB));
	}
	
	public MidiTrack getCurrent() {
		return tracks.getCurrent();
	}
	
	public KnobPanel getKnobs(MidiTrack t) {
		for (TrackKnobs v : knobs)
			if (v.getTrack() == t)
				return v;
		return null;
	}
	
	public MidiTrack get(MidiReceiver rcv) {
		for (MidiTrack t : tracks)
			if (t.getMidiOut() == rcv)
				return t;
		return null;
	}
	
	@Override
	public Iterator<MidiTrack> iterator() {
		return tracks.iterator();
	}

	public MidiTrack get(int idx) {
		return tracks.get(idx);
	}

	public int numTracks() {
		return tracks.size();
	}

	public List<Sched> state() {
		ArrayList<Sched> result = new ArrayList<>();
		for (MidiTrack t : this)
			result.add(new Sched(t.getState()));
		return result;
	}

	public MidiTrack byName(String track) {
		for (MidiTrack t : this)
			if (t.getName().equals(track))
				return t;
		return null;
	}

}
