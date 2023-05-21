package net.judah.seq;

import java.awt.Dimension;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.api.MidiReceiver;
import net.judah.drumkit.KitMode;
import net.judah.gui.Size;
import net.judah.gui.knobs.KnobPanel;
import net.judah.gui.knobs.TrackKnobs;
import net.judah.midi.Midi;
import net.judah.song.*;
import net.judah.util.RTLogger;

@Getter 
public class Seq implements Iterable<MidiTrack>, MidiConstants, Cmdr {
	public static final int TRACKS = 10;
	public static final String NAME = "Seq";
	
	Dimension size = new Dimension(Size.WIDTH_TAB / 3, (Size.HEIGHT_TAB - 50) / 5);

	private final TrackList tracks = new TrackList();
	private final TrackList drumTracks;
	private final TrackList synthTracks;
	private final ArrayList<TrackKnobs> knobs = new ArrayList<>();

	public Seq(TrackList drumTracks, TrackList synthTracks) {
		this.drumTracks = drumTracks;
		drumTracks.forEach(track->track.setCycle(CYCLE.AB));
		this.synthTracks = synthTracks;
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

	public void init(List<Sched> tracks) {
		tracks.clear();
		for (MidiTrack t : this)
			tracks.add(new Sched(t.isDrums())); // ??
	}

	public MidiTrack byName(String track) {
		for (MidiTrack t : this)
			if (t.getName().equals(track))
				return t;
		return null;
	}

	public void loadDrumMachine() {
        MidiTrack drum1 = byName(KitMode.Drum1.name());
        drum1.load(new File(drum1.getFolder(), "Rock1"));
        drum1.getMidiOut().progChange("Pearl");
        MidiTrack drum2 = byName(KitMode.Drum2.name());
        drum2.load(new File(drum2.getFolder(), "Rap1"));
        drum2.getMidiOut().progChange("808");
        MidiTrack hats = byName(KitMode.Hats.name());
        hats.load(new File(hats.getFolder(), "Hats1"));
        hats.getMidiOut().progChange("Hats");
        MidiTrack fills = byName(KitMode.Fills.name());
        fills.load(new File(fills.getFolder(), "Fills1"));
        fills.setCue(CUE.Hot);
        fills.getMidiOut().progChange("VCO");
	}

	public void loadTracks(List<TrackInfo> trax) {
		for (TrackInfo info : trax) {
    		MidiTrack t = byName(info.getTrack());
    		if (t == null) {
    			RTLogger.warn(this, "Missing " + info.getTrack());
    			continue;
    		}
    		
    		if (false == info.getProgram().equals(t.getMidiOut().getProg(t.getCh()))) 
    			t.getMidiOut().progChange(info.getProgram(), t.getCh());
    		if (info.getFile() != null && !info.getFile().isEmpty()) {
    			t.load(new File(info.getFile()));
    		}
    	} 

	}

	/**Perform recording or translate activities on tracks
	 * @param midi user note press 
	 * @return true if any track is recording */
	public boolean rtCheck(Midi midi) {
		for (MidiTrack track : tracks)
			if (track.getRecorder().record(midi))
				return true;
			
		for (MidiTrack track : tracks)
			if (track.getTransposer().setAmount(midi))
				return true;
			
		return false;
	}

	
	@Override
	public String[] getKeys() {
		return IntProvider.instance(1, 64, 1).getKeys();
	}

	@Override
	public String lookup(int value) {
		return "" + value;
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
	
}
