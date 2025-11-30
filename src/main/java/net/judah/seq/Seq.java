package net.judah.seq;

import static net.judah.controllers.MPKTools.drumBank;
import static net.judah.controllers.MPKTools.drumIndex;
import static net.judah.seq.MidiConstants.DRUM_CH;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.sound.midi.InvalidMidiDataException;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.api.ZoneMidi;
import net.judah.drumkit.DrumMachine;
import net.judah.drumkit.DrumType;
import net.judah.gui.Gui;
import net.judah.gui.MainFrame;
import net.judah.gui.Pastels;
import net.judah.gui.Size;
import net.judah.gui.Updateable;
import net.judah.gui.knobs.KnobMode;
import net.judah.gui.knobs.TrackKnobs;
import net.judah.gui.widgets.TrackButton;
import net.judah.midi.JudahMidi;
import net.judah.midi.Midi;
import net.judah.midi.MidiInstrument;
import net.judah.omni.Threads;
import net.judah.sampler.Sampler;
import net.judah.seq.SynthRack.RegisteredSynths;
import net.judah.seq.chords.ChordTrack;
import net.judah.seq.track.ChannelTrack;
import net.judah.seq.track.DrumTrack;
import net.judah.seq.track.MidiTrack;
import net.judah.seq.track.PianoTrack;
import net.judah.seq.track.TrackInfo;
import net.judah.song.Sched;
import net.judah.song.Song;
import net.judah.synth.taco.Polyphony;
import net.judah.synth.taco.TacoSynth;
import net.judah.synth.taco.TacoTruck;
import net.judah.util.Constants;
import net.judah.util.RTLogger;

/** MidiTracks holder */

public class Seq extends JPanel implements Updateable, Iterable<MidiTrack>{

	@Getter private TrackList<MidiTrack> tracks = new TrackList<MidiTrack>();
	@Getter private final TrackList<DrumTrack> drumTracks;
	private final TrackList<ChannelTrack> ccTracks = new TrackList<ChannelTrack>();
	private TrackList<MidiTrack> replace = null;
	private final ChordTrack chords;
	private final Sampler sampler;
	private final DrumMachine drums;
	@Getter private final Clipboard clipboard = new Clipboard();

	// GUI
	private static final Dimension SIZE = new Dimension(Size.WIDTH_KNOBS / 2 , 73);
	private final JPanel dnb = new JPanel(new GridLayout(1, 5, 1, 0));
	private final JPanel synths = new JPanel();
	private final MidiInstrument bass;

	public Seq(DrumMachine drumz, ChordTrack chordTrack, Sampler sampler, MidiInstrument bass) {
		this.chords = chordTrack;
		this.sampler = sampler;
		this.drums = drumz;
		this.bass = bass;

		drumTracks = drums.getTracks();
		tracks.addAll(drumTracks);
		tracks.addAll(SynthRack.getSynthTracks());
		tracks.addAll(ccTracks);
		gui();
	}

	public MidiTrack getCurrent() {
		return tracks.getCurrent();
	}

	@Override public Iterator<MidiTrack> iterator() {
		return tracks.iterator();
	}

	public MidiTrack get(int idx) {
		return tracks.get(idx);
	}

	public int numTracks() {
		return tracks.size();
	}

	/* New Song */
	public void newSong() {
		chords.clear();
		clearSynths();

		SynthRack.getFluids()[0].getTrack().setName("Fluid");
		SynthRack.getOther()[0].getTrack().setName("Bass");
		SynthRack.getTacos()[0].getTrack().setName("Taco");
		SynthRack.getTacos()[1].getTrack().setName("Tk2");
		SynthRack.getFluids()[0].getTrack().setState(new Sched());
		SynthRack.getOther()[0].getTrack().setState(new Sched());
		SynthRack.getTacos()[0].getTrack().setState(new Sched());
		SynthRack.getTacos()[1].getTrack().setState(new Sched());
		updates();
	}

	// New scene legacy
	public void init(List<Sched> tracks) {
		tracks.clear();
		for (int i = 0; i < numTracks(); i++)
			tracks.add(new Sched(get(i).isSynth()));
	}

	private void clearSynths() {
		tracks.clear();
		tracks.addAll(drumTracks);
		// clear previous song Tracks
		for (ZoneMidi engine : SynthRack.engines) {
			Vector<? extends MidiTrack> tracks = engine.getTracks();
			for (int i = tracks.size() - 1; i >= 0; i--)
				if (!tracks.get(i).isPermanent())
				tracks.remove(i);
		}
	}

	/** load song into sequencer */
	public void loadSong(Song song) {
		newSong();
		chords.load(song);
		for (TrackInfo info : song.getTracks()) {
			String name = info.getTrack();
    		MidiTrack t = byName(name);
    		if (t == null) {
    			if (name.equals("B"))
    				t = JudahZone.getBass().getTrack();
    			else if (name.startsWith("F"))
    				t = addTrack(name, RegisteredSynths.Fluid, 0);
//    			else if (name.startsWith("T"))
//    				t = addTrack(name, RegisteredSynths.Taco, 1);
    			else if (info.getFile() == null || info.getFile().isBlank())
    				continue; // ignore unassigned legacy
    			else {
    				RTLogger.warn(this, name + " - Unknown Track: " + info);
    				continue;
    			}
    		}
    		t.load(info);
    	}
	}

	public MidiTrack byName(String track) {
		for (MidiTrack t : this)
			if (t.getName().equals(track))
				return t;

		for (Trax legacy :Trax.values()) // Legacy Trax format
			if (legacy.name().equals(track) || legacy.getName().equals(track))
				return legacy(legacy);

		return null;
	}
	private MidiTrack legacy(Trax type) {
		switch (type) {
			case D1: return drums.getTrack();
			case D2: return drums.getTracks().get(1);
			case H1: return drums.getTracks().get(2);
			case H2: return drums.getTracks().get(3);
			case B: return SynthRack.getOther()[0].getTrack();
//			case F1: return SynthRack.getFluids()[0].getTrack();
			// F2 F3
			case TK1: return SynthRack.getTacos()[0].getTrack();
			case TK2: return SynthRack.getTacos()[1].getTrack();
			default: return null;
		}
	}

	public void step(int step) {
		chords.step(step);
		sampler.step(step);
	}

	// JudahMidi.process() -> clock -> to here
	public void percent(float percent) {
		tracks.forEach(track->track.playTo(percent));
		if (replace != null) {
			tracks = replace;
			replace = null;
			Threads.execute(()->updates());
		}
	}

	/**Performs recording or translate activities on tracks, drum pads are also sounded from here.
	 * @param midi user note press
	 * @return true if drums or a track is recording or transposing (consuming) the note*/
	public boolean captured(Midi midi) {
		boolean result = false;
		if (midi.getChannel() == DRUM_CH) {
			Midi note = translateDrums(midi); // translate from MPK midi to DrumKit midi
			for (DrumTrack t : drumTracks)
				if (t.capture(note))
					result = true;

			if (!result && Midi.isNoteOn(note))
				drums.getChannel(note.getChannel()).send(note, JudahMidi.ticker());
			return true; // all drum pads consumed here
		}

		for (PianoTrack t: SynthRack.getSynthTracks()) {
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


	public void trigger(int trackNum) {
		if (numTracks() > trackNum)
			get(trackNum).trigger();
	}
	@Override public String toString() {
		StringBuffer sb = new StringBuffer(this.getClass().getSimpleName()).append(" Tracks: ").append(Constants.NL);
		sb.append(drumTracks.size()).append(" drums ");
		sb.append(SynthRack.getSynthTracks().size()).append(" synths");
		sb.append(ccTracks.size()).append(" channels.").append(Constants.NL).append(Constants.NL);
		return sb.toString();
	}

	public void addTrack(String name, ZoneMidi engine) {
		try {
			if (engine instanceof TacoTruck truck) {
				TacoSynth taco = new TacoSynth(name, truck, new Polyphony(truck, truck.getTracks().size()));
				truck.getTracks().add(taco);
				taco.getMeta().setString(Meta.DEVICE, "" + RegisteredSynths.Taco.name());
			}
			else {
				((MidiInstrument)engine).getTracks().add(new PianoTrack(name, engine, engine.getTracks().size()));
				engine.getTracks().getLast().getMeta().setString(Meta.DEVICE, "" + RegisteredSynths.Fluid.name());
			}
			refill();
		} catch (InvalidMidiDataException e) { RTLogger.warn(this, e); }
	}

	public PianoTrack addTrack(String name, RegisteredSynths type, int engineIdx) {

		PianoTrack result = null;

		ZoneMidi engine = switch(type) {
			case Taco -> SynthRack.getTacos()[engineIdx];
			case Fluid -> SynthRack.getFluids()[engineIdx];
			case External -> SynthRack.getOther()[engineIdx];
		};
		try {
			if (type == RegisteredSynths.Taco) {
				TacoTruck zynth = (TacoTruck)engine;
				TacoSynth taco = new TacoSynth(name, zynth, new Polyphony(zynth, zynth.getTracks().size()));
				zynth.getTracks().add(taco);
				result = taco;
			}
			else {
				result = new PianoTrack(name, engine, engine.getTracks().size());
				((MidiInstrument)engine).getTracks().add(result);
			}
			refill();
		} catch (InvalidMidiDataException e) { RTLogger.warn(this, e); }
		result.getMeta().setString(Meta.DEVICE, "" + type.name());
		return result;
	}

	public void removeTrack(MidiTrack track) {
		for (ZoneMidi zone : SynthRack.getAll())
			if (zone.getTracks().contains(track)) {
				zone.getTracks().remove(track);
				tracks.removeElement(track);
				updates();
				return;
			}
	}

	private void gui() {
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        Gui.resize(this, SIZE);
        setOpaque(true);
        // D1 D2 D3 D4 B
        // T1 T2 F1 .. Fx
		for (DrumTrack t : drumTracks)
    		dnb.add(new TrackButton(t, this));
        add(dnb);
        add(synths);
        update();
        dnb.setBorder(BorderFactory.createLineBorder(Pastels.MY_GRAY));
	}

	private void updates() {
		// MiniSeq
 		PianoTrack bassTrack = bass.getTrack();
 		if (dnb.getComponents().length == 5) { // 4 drums, 1 bass
 			if ( ((TrackButton)dnb.getComponent(4)).getTrack() != bassTrack) {
 				dnb.remove(4);
 				if (bassTrack != null)
 					dnb.add(new TrackButton(bassTrack, this));
 			}
 		} else if (bassTrack != null)
 			dnb.add(new TrackButton(bassTrack, this));
 		synths.removeAll();
 		for (PianoTrack t : SynthRack.getSynthTracks())
 			if (t != bassTrack)
 				synths.add(new TrackButton(t, this));
 		synths.setLayout(new GridLayout(1, synths.getComponentCount()));

 		// Other updates
 		JudahZone.getOverview().refill();
 		if (MainFrame.getKnobMode() == KnobMode.Track)
 			((TrackKnobs)MainFrame.getKnobs()).refill();
 		JudahZone.getFrame().getMenu().refillTracks();
	}

	void refill() {
		TrackList<MidiTrack> temp = new TrackList<MidiTrack>();
		temp.addAll(drumTracks);
		temp.addAll(SynthRack.getSynthTracks());
		if (JudahZone.getClock().isActive()) {
			replace = temp; // RT atomic
			return;
		}

		tracks = temp;
		updates();
	}

	@Override public void update() {
		for (Component c : synths.getComponents())
			((TrackButton)c).update();
		for (Component c : dnb.getComponents())
			((TrackButton)c).update();
		repaint();
	}
	public void update(MidiTrack t) {
		for (Component c : dnb.getComponents())
			if ( ((TrackButton)c).getTrack() == t)
				((TrackButton)c).update();
		for (Component c : synths.getComponents())
			if ( ((TrackButton)c).getTrack() == t)
				((TrackButton)c).update();
	}

	public void rename(PianoTrack track) {
		String result = Gui.inputBox("New Name");
		if (result == null || result.isBlank())
			return;
		track.setName(result);
		updates();
	}

	public void confirmDelete(PianoTrack track) {
		int result = JOptionPane.showConfirmDialog(JudahZone.getFrame(), "Delete Track " + track.getName() + "?");
		if (result == JOptionPane.YES_OPTION)
			removeTrack(track);
	}

	public void clear(MidiTrack t) { // TODO
		t.clear();
		t.setState(new Sched());
		MainFrame.update(t);
	}

}


// Song Bundle
//private MidiFile s;
//private Track zero;
//
//public int getResolution() { return s.getResolution(); }
// () {
//try {
//	s = new MidiFile();
//	zero = s.createTrack();
//	zero.add(new MidiEvent(new MetaMessage(
//			Meta.COPYRIGHT.type, JUDAHZONE.getBytes(), JUDAHZONE.getBytes().length), 0l));
//} catch (InvalidMidiDataException e) { RTLogger.warn(this, e); }

//public void resolutionView() {
//String result = JOptionPane.showInputDialog(JudahZone.getFrame(),
//		toString() + "New Resolution:", s.getResolution());
//if (result == null) return;
//try { setResolution(Integer.parseInt(result));
//} catch (NumberFormatException e) { RTLogger.log("Resolution", result + ": " + e.getMessage()); }
//}

//public void setResolution(int rez) {
//if (rez < 2 || rez > 2048)
//	throw new NumberFormatException("out of bounds");
//float factor = rez / (float)getResolution();
//for (int i = t.size() - 1; i >= 0; i--) {
//	t.get(i).setTick((long) (t.get(i).getTick() * factor));
//}
//s.setResolution(rez);
//setBarTicks(clock.getTimeSig().beats * rez);
//compute();
//MainFrame.update((Updateable) () -> {
//	if (TabZone.getMusician(MidiTrack.this) != null)
//		TabZone.getMusician(MidiTrack.this).timeSig(clock.getTimeSig());
//	});
//}

//public void loadSong(Song song) {
//for (TrackInfo t :song.getTracks()) {
//	t.getFile()
//
//	t.getTrack()
//}
//
//for (TrackInfo info : trax) {
//	MidiTrack t = byName(info.getTrack());
//	if (t == null) {
//		RTLogger.warn(this, "Unknown Track: " + info.getTrack());
//		continue;
//	}
//	t.load(info);
//}
//}
//public void loadSong(MidiFile file) {
//s = file;
//Track[] in = file.getTracks();
//int size = in.length;
//if (size > 0) {
//	// try to load Track zero
//}
//// look at meta, each track, build track
//for (Track t : in) {
//	String engine = null;
////	String name = null;
//	int port = 0;
//	int ch = 0;
//	MetaMap mine = new MetaMap(t);
//	if (mine.containsKey(Meta.DEVICE) ) {
//		engine = mine.getString(Meta.DEVICE);
//	}
//	if (mine.containsKey(Meta.TRACK_NAME)) {
//		// good!
//	}
//	if (mine.containsKey(Meta.PORT)) {
////		port = mine.getInt(Meta.PORT);
//	}
//	if (mine.containsKey(Meta.CHANNEL)) {
////		ch = mine.getInt(Meta.CHANNEL);
//	}
//}
//}
//public void remove(MidiTrack t) {
//	tracks.remove(t);
//	MainFrame.update(this);
//}

//public void createTrack() {
//	// TACO or Fluid or Automation (or Crave/Etc) (separate channel?)
//	// 1. gather info from user

//	// 2. create T
//	// 3. post MetaInfo
//	// 4. post TrackInfo
//	// 5. update Gui
//}
//public void accept(NewTrack form) {
//	TrackType type = TrackType.values()[form.tabs.getSelectedIndex()];
//	switch(type) {
//		case Drum -> createDrum(form);
//		case Synth -> createSynth(form);
//		case Chord -> addChords(form.chords);
//		case FX -> createChannel(form);
//	}
//}
//
//private void createDrum(NewTrack form) {
//	RegisteredDrums type = RegisteredDrums.values()[form.drumOut.getSelectedIndex()];
//	RTLogger.log(this, "you want a " + type.name());
//	//	jackclient.registerPort("left", AUDIO, JackPortIsOutput);
//}
//private void createSynth(NewTrack form) {
//	SynthRack.RegisteredSynths type = SynthRack.RegisteredSynths.values()[form.synthType.getSelectedIndex()];
//	boolean join = form.synthOut.isEnabled();
//	ZoneMidi target = form.synthOut.getSelectedValue();
//
//	if (join)
//		RTLogger.log(this, "you want to join " + target.getName());
//	else {// TODO extern handling
//		RTLogger.log(this, "you want a " + type.name());
//		if (type == SynthRack.RegisteredSynths.Fluid) {
//			new MakeItRain();
//		}
//	}
//}
//private void createChannel(NewTrack form) {
//	Channel ch = form.fxChannel.getSelectedValue();
//
//	RTLogger.log(this, "you want automation on " + ch.getName());
//
//}
//
//private void addChords(File f) {
//	if (f == null)
//		return;
//	chords.load(f);
//	JudahZone.getOverview().refill();
//}
//

