package net.judah.seq;

import java.awt.Dimension;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;

import lombok.Getter;
import net.judah.api.MidiReceiver;
import net.judah.gui.Size;
import net.judah.gui.knobs.KnobPanel;
import net.judah.gui.knobs.TrackKnobs;
import net.judah.midi.Midi;
import net.judah.song.Sched;
import net.judah.util.Folders;
import net.judah.util.RTLogger;

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

		try {
			scale(tracks.get(4));
		} catch (Exception e) { RTLogger.warn(this, e); }
		
				
		for(MidiTrack track : this) 
			knobs.add(new TrackKnobs(track, this));

		new ImportMidi(tracks.get(0), new File(Folders.getMidi(), "Rap1"), 0);

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
	
	public void process(float beat) {
		for (MidiTrack track : tracks) {
			if (track.isActive()) 
				track.playTo(beat);
		}
	}
	
	public static void scale(MidiTrack track) throws InvalidMidiDataException {
		for (int i = 0; i < 15; i++) {
			track.addEvent(new MidiEvent(Midi.create(NOTE_ON, track.getCh(), i * 2 + 48, 100), 
					i * track.getResolution()), 0);
			track.addEvent(new MidiEvent(Midi.create(NOTE_OFF, track.getCh(), i * 2 + 48, 100), 
					i * track.getResolution() + track.getResolution() - 1), 0);
		}
		track.getScheduler().init();
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


//	public void update() {
////		if (tab != null && tab.isVisible())
////			tab.changeTrack(current.getView());
////		if (isVisible()) 
////			for (MidiPlayer player : views)
////				player.getHighlight().setBackground(player.getTrack() == current ? 
////					Pastels.MY_GRAY : Pastels.PINK);
//	}


	public List<Sched> state() {
		ArrayList<Sched> result = new ArrayList<>();
		for (MidiTrack t : this)
			result.add(new Sched(t.getScheduler().getState()));
		return result;
	}

}
