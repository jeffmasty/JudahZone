package net.judah.drumkit;

import java.util.ArrayList;

import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;

import judahzone.api.Midi;
import judahzone.gui.Icons;
import judahzone.util.AudioTools;
import lombok.Getter;
import net.judah.channel.Channel;
import net.judah.gui.knobs.KitKnobs;
import net.judah.midi.JudahMidi;
import net.judah.seq.TrackList;
import net.judah.seq.Trax;
import net.judah.seq.track.Cue;
import net.judah.seq.track.DrumTrack;
import net.judah.seq.track.MidiTrack;
import net.judah.seq.track.NoteTrack;
import net.judah.synth.Engine;

@Getter
public final class DrumMachine extends Engine {

	private final TrackList<DrumTrack> tracks = new TrackList<DrumTrack>();
	private final ArrayList<DrumKit> kits = new ArrayList<DrumKit>();
	private final KitKnobs knobs;
	private final Channel mains;
	private KitSetup settings = new KitSetup();

	public DrumMachine(Channel mains) throws Exception {
		super("Drums", true);
		this.mains = mains;
		icon = Icons.get("Drums.png");
		for (Drumz type : Drumz.values()) {
			DrumKit kit = new DrumKit(this, type);
			kits.add(kit);
			DrumTrack trak = new DrumTrack(type, kit);
			tracks.add(trak);
			trak.progChange(type.program);
		}
		knobs = new KitKnobs(this);
		gain.setPreamp(0.2f);
	}

	public void init(String preset) {
		setPreset(preset);
		for (int i = 0; i < tracks.size(); i++)
			tracks.get(i).load(Trax.values()[i].getFile());
		tracks.get(Trax.H2.ordinal()).setCue(Cue.Hot);
	}

	public DrumTrack getCurrent() {
		return (DrumTrack)tracks.getCurrent();
	}

	public DrumTrack getTrack(DrumKit kit) {
		for (DrumTrack t : tracks)
			if (t.getKit() == kit)
				return t;
		return null;
	}

	@Override public String[] getPatches() {
		return DrumDB.getKits().toArray(new String[DrumDB.getKits().size()]);
	}

//	@Override public boolean progChange(String preset, int channel) {
//		return getChannel(channel).progChange(preset);
//	}
//
//	@Override public String getProg(int ch) {
//		for (int i = 0; i < tracks.size(); i++)
//			if (tracks.get(i).getCh() == ch)
//				return tracks.get(i).getKit().getProgram().getFolder().getName();
//			return "?";
//	}
//
//	@Override public boolean progChange(String preset) {
//		return tracks.getFirst().getKit().progChange(preset);
//	}
//
	@Override public String progChange(int data2, int ch) {
		if (data2 < 0 || data2 >= DrumDB.getKits().size())
			return null;
		String result = DrumDB.getKits().get(data2);
		getChannel(ch).progChange(result);
		return result;
	}

	@Override public void send(MidiMessage midi, long timeStamp) {
		if (midi instanceof MetaMessage)
			return; // TODO
		ShortMessage m = Midi.copy((ShortMessage)midi);
		if (settings.cc(m))
			return; // filtered envelope cc
		if (Midi.isCC(m)) // bypass mutes to filter channel cc
			getChannel(m.getChannel()).send(m, JudahMidi.ticker());
		else if (onMute || mains.isOnMute())
			return; // discard
		getChannel(m.getChannel()).send(m, JudahMidi.ticker());
	}

	public DrumKit getChannel(int channel) {
		for (MidiTrack t : tracks)
			if (t.getCh() == channel)
				return ((DrumTrack)t).getKit();
		return null;
	}

	public void setCurrent(DrumTrack t) {
		tracks.setCurrent(t);
	}

	public void setCurrent(int idx) {
		if (tracks.size() < idx)
			return;

		tracks.setCurrent(tracks.get(idx));
	}

	@Override public void close() {
		reset();
	}


	///////////////////////////////////////////////
	// process + mix each drumkit, process this channel's fx, place on mains
	@Override
	protected void processImpl() {
		AudioTools.silence(left);
		AudioTools.silence(right);
		for (DrumTrack track : tracks) {
			track.getKit().processImpl();
			AudioTools.mix(track.getKit().getLeft(), left);
			AudioTools.mix(track.getKit().getRight(), right);
		}

		fx();
	}

	@Override
	public NoteTrack getTrack() {
		if (tracks.isEmpty())
			return null;
		return tracks.get(0);
	}


}
