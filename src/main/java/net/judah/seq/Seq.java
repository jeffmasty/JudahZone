package net.judah.seq;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.api.ZoneMidi;
import net.judah.drumkit.DrumMachine;
import net.judah.gui.knobs.KnobPanel;
import net.judah.gui.knobs.TrackKnobs;
import net.judah.midi.JudahClock;
import net.judah.midi.Midi;
import net.judah.mixer.LineIn;
import net.judah.mixer.Zone;
import net.judah.sampler.Sampler;
import net.judah.seq.chords.ChordTrack;
import net.judah.seq.track.MidiTrack;
import net.judah.seq.track.PianoTrack;
import net.judah.seq.track.TrackInfo;
import net.judah.song.Sched;
import net.judah.song.cmd.Cmd;
import net.judah.song.cmd.Cmdr;
import net.judah.song.cmd.IntProvider;
import net.judah.song.cmd.Param;
import net.judah.util.RTLogger;

/** Sequencer */
@Getter
public class Seq implements Iterable<MidiTrack>, MidiConstants, Cmdr {
	public static final int TRACKS = 10;

	private final TrackList tracks = new TrackList();
	private final TrackList drumTracks = new TrackList();
	private final TrackList synthTracks = new TrackList();
	private final ArrayList<TrackKnobs> knobs = new ArrayList<>();
	private final ChordTrack chords;
	private final Sampler sampler;

	public Seq(Zone instruments, ChordTrack chordTrack, Sampler sampler, JudahClock clock) {
		this.chords = chordTrack;
		this.sampler = sampler;
		for (LineIn line : instruments) {
			if (line instanceof DrumMachine)
				for (MidiTrack track : ((DrumMachine)line).getTracks())
					drumTracks.add(track);

			else if (line instanceof ZoneMidi)
				for (MidiTrack t : ((ZoneMidi)line).getTracks())
					synthTracks.add(t);
		}
		tracks.addAll(drumTracks);
		tracks.addAll(synthTracks);

		for(MidiTrack track : this)
			knobs.add(new TrackKnobs(track, this));

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

	public MidiTrack lookup(ZoneMidi rcv, int ch) {
		for (MidiTrack t : tracks)
			if (t.getMidiOut() == rcv && t.getCh() == ch)
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

	public void init(List<Sched> tracks) {
		tracks.clear();
		for (int i = 0; i < TRACKS; i++)
			tracks.add(new Sched(get(i).isSynth()));
	}

	public MidiTrack byName(String track) {
		for (MidiTrack t : this)
			if (t.getName().equals(track))
				return t;
		return null;
	}



	/** load song into sequencer */
	public void loadSong(List<TrackInfo> trax) {
		for (TrackInfo info : trax) {
    		MidiTrack t = byName(info.getTrack());
    		if (t == null) {
    			RTLogger.warn(this, "Unknown Track: " + info.getTrack());
    			continue;
    		}
    		t.load(info);
    	}
	}

	/**Performs recording or translate activities on tracks
	 * @param midi user note press
	 * @return true if any track is recording or transposing*/
	public boolean arpCheck(Midi midi) {
		if (midi.getChannel() >= MidiConstants.DRUM_CH)
			return false;
		for (MidiTrack track : synthTracks)
			if (((PianoTrack)track).getArp().mpkFeed(midi))
				return true;
		return false;
	}

	@Override
	public String[] getKeys() {
		return IntProvider.instance(1, 64, 1).getKeys();
	}

	@Override
	public Integer resolve(String key) {
		return Integer.parseInt(key);
	}

	@Override
	public void execute(Param p) {
		if (p.cmd == Cmd.Length)
				JudahZone.getClock().setLength(Integer.parseInt(p.val));
	}

	public void step(int step) {
		chords.step(step);
		sampler.step(step);
	}

	public void percent(float percent) {
		tracks.forEach(track->track.playTo(percent));
	}

}
