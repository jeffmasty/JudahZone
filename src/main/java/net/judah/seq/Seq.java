package net.judah.seq;

import static net.judah.controllers.MPKTools.drumBank;
import static net.judah.controllers.MPKTools.drumIndex;
import static net.judah.seq.MidiConstants.DRUM_CH;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.api.ZoneMidi;
import net.judah.drumkit.DrumMachine;
import net.judah.drumkit.DrumType;
import net.judah.gui.knobs.KnobPanel;
import net.judah.gui.knobs.TrackKnobs;
import net.judah.midi.JudahMidi;
import net.judah.midi.Midi;
import net.judah.sampler.Sampler;
import net.judah.seq.chords.ChordTrack;
import net.judah.seq.track.DrumTrack;
import net.judah.seq.track.MidiTrack;
import net.judah.seq.track.PianoTrack;
import net.judah.seq.track.TrackInfo;
import net.judah.song.Sched;
import net.judah.song.cmd.Cmd;
import net.judah.song.cmd.Cmdr;
import net.judah.song.cmd.IntProvider;
import net.judah.song.cmd.Param;
import net.judah.synth.taco.TacoTruck;
import net.judah.util.RTLogger;

/** MidiTracks holder */
@Getter
public class Seq implements Iterable<MidiTrack>, Cmdr {

	public static final int TRACKS = Trax.values().length;

	private final TrackList<MidiTrack> tracks = new TrackList<MidiTrack>();
	private final TrackList<DrumTrack> drumTracks = new TrackList<DrumTrack>();
	private final TrackList<PianoTrack> synthTracks = new TrackList<PianoTrack>();
	private final ArrayList<TrackKnobs> knobs = new ArrayList<>();
	private final ChordTrack chords;
	private final Sampler sampler;
	private final DrumMachine drums;

	public Seq(DrumMachine drumz, ZoneMidi bass, TacoTruck tacos, ChordTrack chordTrack, Sampler sampler) {
		this.chords = chordTrack;
		this.sampler = sampler;
		this.drums = drumz;
		drums.getTracks().forEach(t->drumTracks.add(t));
		synthTracks.add((PianoTrack) bass.getTracks().getFirst());
		tacos.tracks.forEach(taco->synthTracks.add(taco.getTracks().getFirst()));
		tacos.fluid.getTracks().forEach(t->synthTracks.add(t));
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
		for (int i = 0; i < numTracks(); i++)
			tracks.add(new Sched(get(i).isSynth()));
	}

	public MidiTrack byName(String track) {
		for (MidiTrack t : this) {
			if (t.getName().equals(track))
				return t;
		}
		for (Trax legacy :Trax.values()) // Legacy song saves
			if (legacy.getName().equals(track))
				return byName(legacy.toString());
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

	/**Performs recording or translate activities on tracks, drum pads are also sounded from here.
	 * @param midi user note press
	 * @return true if drums or a track is recording or transposing (consuming) the note*/
	public boolean captured(Midi midi) {
		boolean result = false;
		if (midi.getChannel() == DRUM_CH) {
			Midi note = translateDrums(midi); // translate from MPK midi to DrumKit midi
			for (DrumTrack t : drumTracks) {
				if (t.capture(note))
					result = true;
			}
			if (!result && Midi.isNoteOn(note))
				drums.getChannel(note.getChannel()).send(note, JudahMidi.ticker());
			return true; // all drum pads consumed here
		}

		for (PianoTrack t: synthTracks) {
			if (t.capture(midi))
				result = true;
			else if (t.isMpkOn()) {
				t.mpk(Midi.copy(midi));
				result = true;
			}
		}
		return result;
	}

	public Midi translateDrums(Midi midi) {
		return Midi.create(midi.getCommand(), DRUM_CH + drumBank(midi.getData1()),
				DrumType.values()[drumIndex(midi.getData1())].getData1(), midi.getData2());
	}

}



