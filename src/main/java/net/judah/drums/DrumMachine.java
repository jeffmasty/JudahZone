package net.judah.drums;

import java.io.File;
import java.util.ArrayList;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;

import judahzone.api.Midi;
import judahzone.api.MidiUpdate;
import judahzone.drums.DrumDB;
import judahzone.gui.Icons;
import judahzone.util.AudioTools;
import lombok.Getter;
import net.judah.gui.MainFrame;
import net.judah.midi.JudahMidi;
import net.judah.seq.TrackList;
import net.judah.seq.chords.Chords;
import net.judah.seq.track.DrumTrack;
import net.judah.seq.track.MidiTrack;
import net.judah.seq.track.NoteTrack;
import net.judah.seq.track.PianoTrack;
import net.judah.synth.SynthEngine;


/*
SystemKits (4)   1 FM, 1 Pluck, 1 Noise, 1 SampleKit (/resources 808)

SampleKit: 8 .wav files
SampleKitDB  holds (user) sample kits


SampleKit not used directly.  just for reading off disk, lookup in DB by  kitName + drumType to satisfy param mapping


UserKits:   json (8):  type, registration, param map etc
		if reg=wav them  param=sampleKit name

A Drumset is the live drum model.   Drumsets based off of SampleKit are 8 jsons pointing to reg:wav + sample=kitName
so, DrumDB will hold jsons 4 (system kits) + wav kits (sampleKits) + user Kits (json defined)

try to map to GM drum:
 */
/*	1	Standard Drum Kit  SampleKits 0-8  (~system-ish)
9	Room Drum Kit    (reverbs)  (808 on preset reverbs/delays?)
17	Power Drum Kit      Noise (system)
25	Electric Drum Kit   FM  (system)
26	Rap TR808 Drums    808 again 909 ufo ...(wav)
33	Jazz Drum Kit      Pluck (system)
41	Brush Kit          Hats (system)
50 to 127 user by createDate */
public final class DrumMachine extends SynthEngine {

	public static final int HARD_CODED = 4;

	@Getter private final TrackList<DrumTrack> tracks = new TrackList<DrumTrack>();
	@Getter private final ArrayList<Drumset> kits = new ArrayList<Drumset>();

	private final DrumDB drumDB = new DrumDB();


	public DrumMachine(File drumsets) throws Exception {
		super("Drums", true);
		icon = Icons.get("Drums.png");
		drumDB.load(drumsets);
		setOnMixer(true);
	}

	public DrumTrack getCurrent() {
		return (DrumTrack)tracks.getCurrent();
	}

	public DrumTrack getTrack(Drumset kit) {
		for (DrumTrack t : tracks)
			if (t.getKit() == kit)
				return t;
		return null;
	}

	@Override public String[] getPatches() {
		return drumDB.getPatches();
	}

	@Override public String progChange(int data1, int ch) {
		return getChannel(ch).progChange(data1);
	}

	@Override public int progChange(String preset, int ch) {
		String[] p = drumDB.getPatches();
		for (int i = 0; i < p.length; i++) {
			if (p[i].equals(preset)) {
				progChange(i, ch);
				return i;
			}
		}
		return -1;
	}

	@Override public void send(MidiMessage midi, long timeStamp) {
		if (midi instanceof MetaMessage)
			return; // TODO
		ShortMessage m = Midi.copy((ShortMessage)midi);
//		if (settings.cc(m))
//			return; // filtered envelope cc
		if (Midi.isCC(m)) // bypass mutes to filter channel cc
			getChannel(m.getChannel()).send(m, JudahMidi.ticker());
		else if (onMute) /*  || mains.isOnMute() */
			return; // discard
		getChannel(m.getChannel()).send(m, JudahMidi.ticker());
	}

	public Drumset getChannel(int channel) {
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
	// process + mix each drumkit, process this channel's fx, place on DrumMachine outs
	@Override
	protected void processImpl() {
		if (onMute) return;

		AudioTools.silence(left);
		AudioTools.silence(right);

		int size = tracks.size();
		for (int i = 0; i < size; i++) {
			Drumset kit = tracks.get(i).getKit();
			kit.processImpl();
			// Mix immediately (while buffers in L1 cache)
			AudioTools.mix(kit.getLeft(), left);
			AudioTools.mix(kit.getRight(), right);
		}

		fx();
	}
	@Override
	public NoteTrack getTrack() {
		if (tracks.isEmpty())
			return null;
		return tracks.get(0);
	}

	String[] names = {"Drum1", "Drum2", "Drum3", "Drum4"};
	String[] files = { "Rock1", "Bossa1", "Hats1", "Fills1" };
	String PRESET = "Drumz";

	public void init(MainFrame ping) {
        for (int i = 0; i < HARD_CODED; i++) {
            Drumset kit = new Drumset(names[i], drumDB, i, ping);
            kits.add(kit);
            DrumTrack t = new DrumTrack(kit.getName(), Midi.DRUM_CH + i,  kit);
            tracks.add(t);
			t.load(files[i]);
        }
		setPreset(PRESET);
	}

	@Override public PianoTrack addTrack(String name, MidiUpdate gui, Chords c) throws InvalidMidiDataException {
		throw new InvalidMidiDataException("DrumMachine does not support adding tracks");
	}

	@Override
	public String getProgram(int ch) {
		if (ch >= Midi.DRUM_CH)
			ch -= Midi.DRUM_CH;
		if (ch >= HARD_CODED)
			return null;
		return getChannel(ch).getProgram();
	}

	@Override
	public int getProgChange(int ch) {
		if (ch >= Midi.DRUM_CH)
			ch -= Midi.DRUM_CH;
		if (ch >= HARD_CODED)
			return -1;
		return getChannel(ch).getProgChange();
	}

}
