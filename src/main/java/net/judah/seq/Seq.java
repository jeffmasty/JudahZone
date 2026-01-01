package net.judah.seq;

import static net.judah.controllers.MPKTools.drumBank;
import static net.judah.controllers.MPKTools.drumIndex;
import static net.judah.seq.MidiConstants.DRUM_CH;
import static net.judah.seq.MidiTools.meta;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.io.File;
import java.time.Year;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Track;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import judahzone.api.Midi;
import judahzone.gui.Gui;
import judahzone.gui.Pastels;
import judahzone.gui.Updateable;
import judahzone.util.Constants;
import judahzone.util.Folders;
import judahzone.util.RTLogger;
import judahzone.util.Threads;
import lombok.Getter;
import net.judah.JudahZone;
import net.judah.channel.Channel;
import net.judah.drumkit.DrumMachine;
import net.judah.drumkit.DrumType;
import net.judah.gui.MainFrame;
import net.judah.gui.Size;
import net.judah.gui.knobs.KnobMode;
import net.judah.gui.knobs.TrackKnobs;
import net.judah.midi.Actives;
import net.judah.midi.JudahClock;
import net.judah.midi.JudahMidi;
import net.judah.midi.MidiInstrument;
import net.judah.sampler.Sampler;
import net.judah.seq.SynthRack.RegisteredSynths;
import net.judah.seq.automation.Automation;
import net.judah.seq.chords.Chords;
import net.judah.seq.track.ChannelTrack;
import net.judah.seq.track.Computer;
import net.judah.seq.track.Computer.Update;
import net.judah.seq.track.DrumTrack;
import net.judah.seq.track.MidiFile;
import net.judah.seq.track.MidiTrack;
import net.judah.seq.track.NoteTrack;
import net.judah.seq.track.PianoTrack;
import net.judah.seq.track.TrackInfo;
import net.judah.song.Sched;
import net.judah.song.Song;
import net.judah.song.TraxCombo;
import net.judah.synth.ZoneMidi;
import net.judah.synth.fluid.FluidAssistant;
import net.judah.synth.fluid.FluidSynth;
import net.judah.synth.taco.Polyphony;
import net.judah.synth.taco.TacoSynth;
import net.judah.synth.taco.TacoTruck;

/** Midi Sequencer, MidiTracks container for song */
public class Seq extends Gui.Opaque implements Updateable, Iterable<MidiTrack>{
	private static final Dimension SIZE = new Dimension(Size.WIDTH_KNOBS / 2 , 73);

	@Getter private TrackList<MidiTrack> tracks = new TrackList<MidiTrack>();
	private final TrackList<DrumTrack> drumTracks;
	private final TrackList<ChannelTrack> ccTracks = new TrackList<ChannelTrack>();
	@Getter private final ChannelTrack mains;

	private TrackList<MidiTrack> replace = null;
	private final JudahZone zone;
	private final Chords chords;
	private final Sampler sampler;
	private final DrumMachine drums;

	private final JPanel dnb = new JPanel(new GridLayout(1, 5, 1, 0));
	private final JPanel synths = new JPanel();
	private final MidiInstrument bass;
	private MidiFile bundle;
	public int getResolution() { return bundle == null ? 0 : bundle.getResolution(); }

	@Getter private final TraxCombo trax;
	@Getter private final Automation automation;

	public Seq(JudahZone judahZone) {// DrumMachine drumz, Chords chordTrack, Sampler sampler, MidiInstrument bass, Mains outLR) {
		this.zone = judahZone;
		this.chords = zone.getChords();
		this.sampler = zone.getSampler();
		this.drums = zone.getDrumMachine();
		this.bass = zone.getBass();


		drumTracks = drums.getTracks();
		tracks.addAll(drumTracks);
		tracks.addAll(SynthRack.getSynthTracks());
		tracks.addAll(ccTracks);
		ChannelTrack temp = null;
		try {
			temp = new ChannelTrack(zone.getMains(), 0, zone.getMains().getName());
			byte[] copyright = new String(JudahZone.JUDAHZONE + " " + Year.now().getValue()).getBytes();
			temp.getT().add(meta(Meta.COPYRIGHT, copyright, 0l));
			temp.setPermanent(true);
		} catch (InvalidMidiDataException e) { RTLogger.warn(this, e); }
		mains = temp;
		gui();
		trax = new TraxCombo(this);
		automation = new Automation(trax, getCurrent());
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
		ccTracks.clear();
		mains.clear();
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

		// clear previous song variable Tracks
		for (ZoneMidi engine : SynthRack.engines) {
			Vector<? extends MidiTrack> cars = engine.getTracks();
			for (int i = cars.size() - 1; i >= 0; i--) {
				MidiTrack t = cars.get(i);
				if (t.isPermanent())
					tracks.add(t);
				else
					cars.remove(i);
			}
		}
	}

	/** load song into sequencer
	 * @return true if the provided song is a SmashHit */
	public boolean loadSong(Song song) {
		newSong();
		boolean standalone = song.getBundle() == null || song.getBundle().isBlank();
		MainFrame.setBundle(!standalone);
		if (standalone)
			trackInfo(song.getTracks());
		else
			loadBundle(song.getBundle());
		return !standalone;
	}

	private void trackInfo(List<TrackInfo> tracks) {
		for (TrackInfo info : tracks) {
			String name = info.getTrack();
    		MidiTrack t = byName(name);
    		if (t == null) {
    			String ch = info.getChannel();
    			if (ch != null) {
    				Channel channel = zone.getMixer().byName(ch);
    				if (channel == null) {
    					RTLogger.warn(this, "Missing ChannelTrack: " + ch);
    					return;
    				}
    				t = addTrack(channel, name);
    			}
    			else if (name.equals("B"))
    				t = bass.getTrack();
    			else if (name.startsWith("F"))
    				t = addTrack(name, RegisteredSynths.Fluid, 0);
    			else if (name.startsWith("T"))
    				t = addTrack(name, RegisteredSynths.Taco, 1);
    			else if (info.getFile() == null || info.getFile().isBlank())
    				continue; // ignore unassigned legacy
    			else {
    				RTLogger.warn(this, name + " - Unknown Track: " + info);
    				continue;
    			}
    		}
    		if (info != null)
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
			case F1: return SynthRack.getFluids()[0].getTrack();
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
		mains.playTo(percent);

		TrackList<MidiTrack> pending = replace;
	    if (pending != null) {
	        tracks = pending;     // volatile write publishes the new list
	        replace = null;       // clear the pending snapshot
	        Threads.execute(this::updates);
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

	public ChannelTrack addTrack(Channel ch, String trackName) {
		try {
			ChannelTrack result = new ChannelTrack(ch, ccTracks.size(), trackName);
			ccTracks.add(result);
			refill();
			return result;
		} catch (InvalidMidiDataException e) {  RTLogger.warn(this, e); }
		return null;
	}

	public void addTrack(MetaMap map, ZoneMidi engine) {
		int ch = map.getInt(Meta.CHANNEL);
		try {
			PianoTrack target = null;
			if (engine instanceof TacoTruck truck) {
				while (truck.getTracks().size() < ch)
					truck.addTrack("temp");
				target = truck.getTracks().get(ch);
			} else if (engine instanceof FluidSynth fluid) {
				while(fluid.getTracks().size() < ch)
					addTrack("temp", fluid);
				target = fluid.getTracks().get(ch);
			}
			target.importTrack(map, getResolution());
			if (engine instanceof FluidSynth)
				refill();
		} catch (InvalidMidiDataException e) { RTLogger.warn(this, e); }
	}

	public void addTrack(String name, ZoneMidi engine) {
		try {
			if (engine instanceof TacoTruck truck) {
				TacoSynth taco = new TacoSynth(name, truck, new Polyphony(truck, truck.getTracks().size()));
				truck.getTracks().add(taco);
			}
			else {
				((MidiInstrument)engine).getTracks().add(
						new PianoTrack(name, new Actives(engine, engine.getTracks().size()), chords));
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
				result = new PianoTrack(name,
						new Actives(engine, engine.getTracks().size()), chords);
				((MidiInstrument)engine).getTracks().add(result);
			}
			refill();
		} catch (InvalidMidiDataException e) { RTLogger.warn(this, e); }
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

		trax.refill(getTracks(), getCurrent());

 		// Other updates
 		zone.getOverview().refill();
 		if (MainFrame.getKnobMode() == KnobMode.Track)
 			((TrackKnobs)MainFrame.getKnobs()).refill(tracks);
 		zone.getFrame().getMenu().refillTracks();
	}

	void refill() {
		TrackList<MidiTrack> temp = new TrackList<MidiTrack>();
		temp.addAll(drumTracks);
		temp.addAll(SynthRack.getSynthTracks());
		temp.addAll(ccTracks);
		if (JudahMidi.getClock().isActive()) {
			replace = temp; // RT atomic
			return;
		}
		tracks = temp;
		updates();
	}

	public void rename(MidiTrack track) {
		String result = Gui.inputBox("New Name");
		if (result == null || result.isBlank())
			return;
		track.setName(result);
		updates();
	}

	public void confirmDelete(PianoTrack track) {
		int result = JOptionPane.showConfirmDialog(zone.getFrame(), "Delete Track " + track.getName() + "?");
		if (result == JOptionPane.YES_OPTION)
			removeTrack(track);
	}

	public void clear(MidiTrack t) { // TODO
		t.clear();
		t.setState(new Sched());
		MainFrame.update(t);
	}

	public void resolutionView() {
		String result = JOptionPane.showInputDialog(zone.getFrame(),
				toString() + "New Resolution:", bundle.getResolution());
		if (result == null) return;
		try {
			setResolution(Integer.parseInt(result));
		} catch (NumberFormatException e) { RTLogger.log("Resolution", result + ": " + e.getMessage()); }
	}

	void setResolution(int rez) {
		if (rez < 2 || rez > 2048)
			throw new NumberFormatException(rez + " resolution out of bounds");
		mains.setResolution(rez);
		for (MidiTrack notes : tracks)
			if (notes.getResolution() != rez)
				notes.setResolution(rez);
		if (bundle != null)
			bundle.setResolution(rez);
	}

	/* for each legacy track, create a resolved midiTrack inside the returned MidiFile */
	public MidiFile bundle(String song) throws InvalidMidiDataException {
		int rez = JudahClock.MIDI_24;
		for (MidiTrack t : tracks)
			if (t.getResolution() > rez)
				rez = t.getResolution();
		bundle = new MidiFile(rez);

		Track zero = bundle.createTrack();
		if (mains != null)
			MidiTools.copy(mains, zero);
		zero.add(meta(Meta.TRACK_NAME, song.getBytes(), 0));

		for (MidiTrack legacy : tracks) {
			Track dest = bundle.createTrack();
			if (legacy.getResolution() != rez)
				legacy.setResolution(rez);
			MidiTools.copy(legacy, dest);
			// Meta
			String engine = "Bass";
			Byte port = 0;
			if (legacy instanceof DrumTrack d) {
				engine = drums.getName();
				port = (byte) drums.getTracks().indexOf(d);
			} else if (legacy instanceof TacoSynth t) {
				engine = RegisteredSynths.Taco.name();
				TacoTruck[] trucks = SynthRack.getTacos();
				for (byte i = 0; i < trucks.length; i++)
					if (trucks[i].getTracks().contains(t)) {
						port = i;
						break;
					}
			} else if (legacy instanceof PianoTrack p) {
				if (p.getMidiOut() instanceof FluidSynth) {
					engine = RegisteredSynths.Fluid.name();
					FluidSynth[] fluids = SynthRack.getFluids();
					for (byte i = 0; i < fluids.length; i++)
						if (fluids[i].getTracks().contains(p)) {
							port = i;
							break;
						}
				}
			} else if (legacy instanceof ChannelTrack ch) {
				engine = KnobMode.Autom8.name();
				byte[] instrument = ch.getChannel().getName().getBytes();
				dest.add(meta(Meta.INSTRUMENT, instrument, 0));
			}

			byte[] name = legacy.getName().getBytes();
			byte[] cue = legacy.getCue().name().getBytes();

			dest.add(meta(Meta.DEVICE,  engine.getBytes(), 0l));
			dest.add(meta(Meta.PORT, new byte[] {port}, 0l));
			dest.add(meta(Meta.TRACK_NAME, name, 0l));
			dest.add(meta(Meta.CHANNEL, new byte[] {(byte) legacy.getCh()}, 0l));
			dest.add(meta(Meta.CUE, cue, 0l));
		}

		return bundle;
	}

	public void loadBundle(String uuid) {
		File smashHit = new File(Folders.getMidi(), uuid);
		if (smashHit.isFile() == false) {
			RTLogger.warn(this, "Missing Bundle " + uuid);
			return;
		}
		try {
			bundle = new MidiFile(MidiSystem.getSequence(smashHit));
			clearSynths();
			for (Track t : bundle.getTracks()) {
				MetaMap map = new MetaMap(t);
				String device = map.getString(Meta.DEVICE);
				if (device == null)
					device = "Bass";
				if (device.equals(drums.getName()))
					allocateDrum(map);
				else if (device.equals(RegisteredSynths.Taco.name()))
					allocateTaco(map);
				else if (device.equals(RegisteredSynths.Fluid.name()))
					allocateFluid(map);
				else if (device.equals("Bass"))
					allocateBass(map);
				else // device = Autom8
					allocateChannel(map);
			}
			refill();
		} catch (Exception e) {RTLogger.warn(this, e); }

	}

	void allocateDrum(MetaMap map) {
		int ch = map.getInt(Meta.CHANNEL);
		if (ch >= 0 && ch < drums.getTracks().size()) {
			DrumTrack target = drums.getTrack(drums.getChannel(ch));
			target.importTrack(map, getResolution());
		}
	}

	void allocateTaco(MetaMap map) {
		int port = map.getInt(Meta.PORT);
		while (SynthRack.getTacos().length < port)
			SynthRack.makeTaco();
		TacoTruck truck = SynthRack.getTacos()[port];
		addTrack(map, truck);
 	}

	void allocateFluid(MetaMap map) {
		int port = map.getInt(Meta.PORT);

		int deficit = port - SynthRack.getFluids().length;
		if (deficit > 0) {
			for (int i = 1; i <= deficit; i++) {
				String name = "F" + i;
				if (i == deficit)
					new FluidAssistant(name, map);
				else
					new FluidAssistant(name);
			}
		}
	}

	void allocateBass(MetaMap map) {
		bass.getTrack().importTrack(map, getResolution());
	}

	void allocateChannel(MetaMap map) {
		String channel = map.getString(Meta.INSTRUMENT);
		if (channel == null || channel.isBlank())
			return;
		Channel fx = zone.getMixer().byName(channel);
		if (fx == null) {
			RTLogger.warn(this, "Missing ChannelTrack: " + channel);
			return;
		}
		String name = map.getString(Meta.TRACK_NAME);
		if (name == null || name.isBlank())
			name = channel;
		addTrack(fx, name);
	}

	public TrackKnobs getKnobs(MidiTrack track) {
		if (track instanceof NoteTrack notes) {
			if (notes.getTrackKnobs() == null)
				notes.setTrackKnobs(new TrackKnobs(notes, tracks));
			return notes.getTrackKnobs();
		}
		return null;
	}


	public TrackKnobs getKnobs() {
		return getKnobs(tracks.getCurrent());
	}

	@Override public void update() {
		for (Component c : synths.getComponents())
			((TrackButton)c).update();
		for (Component c : dnb.getComponents())
			((TrackButton)c).update();
		repaint();
	}

	public void update(Computer t) {
		for (Component c : dnb.getComponents())
			if ( ((TrackButton)c).getTrack() == t)
				((TrackButton)c).update();
		for (Component c : synths.getComponents())
			if ( ((TrackButton)c).getTrack() == t)
				((TrackButton)c).update();
	}

	public void update(Update type, Computer c) {

		if (c instanceof NoteTrack track) {
			if (track.getTrackKnobs() != null)
				track.getTrackKnobs().update(type);
		}
		if (type == Update.PLAY || type == Update.CAPTURE)
			update(c);
	}

	public boolean confirmBundle() {

		int rez = JudahClock.MIDI_24;
		for (MidiTrack t : tracks)
			if (t.getResolution() > rez)
				rez = t.getResolution();

		String result =  JOptionPane.showInputDialog(zone.getFrame(),
				"Song Bundle: New Resolution", "" + rez);
		if (result == null || result.isBlank())
			return false;

		try {
			setResolution(Integer.parseInt(result));
			return true;
		} catch (NumberFormatException e) {
			RTLogger.log(this, result + " " + e.getMessage());
			return false;
		}
	}

}

