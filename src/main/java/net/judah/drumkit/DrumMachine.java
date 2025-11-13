package net.judah.drumkit;

import java.nio.FloatBuffer;
import java.util.ArrayList;

import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;

import lombok.Getter;
import net.judah.api.Engine;
import net.judah.gui.knobs.KitKnobs;
import net.judah.midi.JudahClock;
import net.judah.midi.JudahMidi;
import net.judah.midi.Midi;
import net.judah.mixer.Channel;
import net.judah.omni.AudioTools;
import net.judah.omni.Icons;
import net.judah.seq.TrackList;
import net.judah.seq.Trax;
import net.judah.seq.track.Cue;
import net.judah.seq.track.DrumTrack;
import net.judah.seq.track.MidiTrack;

@Getter
public final class DrumMachine extends Engine {

	private final TrackList<DrumTrack> tracks = new TrackList<DrumTrack>();
	private final ArrayList<DrumKit> kits = new ArrayList<DrumKit>();
	private final KitKnobs knobs;
	private final Channel mains;
	private KitSetup settings = new KitSetup();

	public DrumMachine(JudahClock clock, Channel mains) throws Exception {
		super("Drums", true);
		this.mains = mains;
		icon = Icons.get("DrumMachine.png");

		for (Trax type : Trax.drums) {
			DrumKit kit = new DrumKit(this, type);
			kits.add(kit);
			tracks.add(new DrumTrack(type, kit, clock));
		}
		knobs = new KitKnobs(this);
	}

	public void init(String preset) {
		setPreset(preset);
		for (DrumTrack t : tracks)
			t.load(t.getType().getFile());
		tracks.get(Trax.H2).setCue(Cue.Hot);
	}

	public DrumTrack getCurrent() {
		return (DrumTrack)tracks.getCurrent();
	}

	@Override public boolean progChange(String preset, int channel) {
		return getChannel(channel).progChange(preset);
	}

	@Override public String[] getPatches() {
		return DrumDB.getKits().toArray(new String[DrumDB.getKits().size()]);
	}

	@Override public String getProg(int ch) {
		for (int i = 0; i < tracks.size(); i++)
			if (tracks.get(i).getCh() == ch)
				return tracks.get(i).getKit().getProgram().getFolder().getName();
			return "?";
	}

	@Override public boolean progChange(String preset) {
		return tracks.getFirst().getKit().progChange(preset);
	}

	@Override public String progChange(int data2, int ch) {
		if (data2 < 0 || data2 >= DrumDB.getKits().size())
			return null;
		String result = DrumDB.getKits().get(data2);
		progChange(result, ch);
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

	public void setCurrent(Trax type) {
		for (DrumTrack t : tracks)
			if (type == t.getType())
				tracks.setCurrent(t);
	}

	@Override public void close() {
		reset();
	}


	///////////////////////////////////////////////
	// process + mix each drumkit, process this channel's fx, place on mains
	@Override
	public void process(FloatBuffer outLeft, FloatBuffer outRight) {
		AudioTools.silence(left);
		AudioTools.silence(right);
		for (DrumTrack track : tracks)
			track.getKit().process(left, right);
		fx();
		AudioTools.mix(left, outLeft);
		AudioTools.mix(right, outRight);
	}


}
