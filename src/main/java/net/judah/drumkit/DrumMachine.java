package net.judah.drumkit;

import java.nio.FloatBuffer;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;

import lombok.Getter;
import net.judah.api.Engine;
import net.judah.gui.knobs.KitKnobs;
import net.judah.gui.knobs.KnobMode;
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

	private final Channel mains;

	/** current midi controller input and view */
	private KitKnobs focus;
	private final KnobMode knobMode = KnobMode.Kitz;

	@Getter TrackList<DrumTrack> tracks = new TrackList<DrumTrack>();

	public DrumMachine(JudahClock clock, Channel mains) throws Exception {
		super("Drums", true);
		this.mains = mains;
		icon = Icons.get("DrumMachine.png");

		for (Trax type : Trax.drums) {
			DrumKit kit = new DrumKit(this, type);
			tracks.add(new DrumTrack(type, kit, clock));
		}
		focus = tracks.getFirst().getKit().getKnobs();
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

	public KitKnobs getKnobs() {
		return getCurrent().getKit().getKnobs();
	}

	public KitKnobs getKnobs(Trax mode) {
		for (int i = 0; i < tracks.size(); i++) {
			if (tracks.get(i).getType() == mode)
				return tracks.get(i).getKit().getKnobs();
		}
		return tracks.getFirst().getKit().getKnobs(); // error
	}

	@Override
	public void reset() {
		super.reset();
		for(MidiTrack t : tracks)
			for (DrumSample s : ((DrumTrack)t).getKit().getSamples())
				s.reset();
	}

	public void increment() {
		int idx = tracks.indexOf(getCurrent()) + 1;
		if (idx >= tracks.size())
			idx = 0;
		tracks.setCurrent(tracks.get(idx));
	}

	@Override
	public boolean progChange(String preset, int channel) {
		return getChannel(channel).progChange(preset);
	}

	@Override
	public String[] getPatches() {
		return DrumDB.getKits().toArray(new String[DrumDB.getKits().size()]);
	}

	@Override
	public String getProg(int ch) {
		for (int i = 0; i < tracks.size(); i++)
			if (tracks.get(i).getCh() == ch)
				return tracks.get(i).getKit().getProgram().getFolder().getName();
			return "?";
	}

	@Override
	public boolean progChange(String preset) {
		return tracks.getFirst().getKit().progChange(preset);
	}

	@Override
	public void send(MidiMessage message, long timeStamp) {
		if (onMute || mains.isOnMute())
			return; // discard
		if (false == Midi.isNoteOn(message))
			return;
		ShortMessage midi = Midi.copy((ShortMessage)message);
		getChannel(midi.getChannel()).send(midi, JudahMidi.ticker());
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

	@Override
	public void close() {
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
