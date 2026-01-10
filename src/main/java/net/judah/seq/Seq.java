package net.judah.seq;

import static judahzone.api.MidiConstants.DRUM_CH;
import static net.judah.controllers.MPKTools.drumBank;
import static net.judah.controllers.MPKTools.drumIndex;
import static net.judah.seq.track.MidiTools.meta;

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
import javax.swing.SwingUtilities;

import judahzone.api.Midi;
import judahzone.gui.Gui;
import judahzone.gui.Pastels;
import judahzone.gui.Updateable;
import judahzone.util.Constants;
import judahzone.util.Folders;
import judahzone.util.RTLogger;
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
import net.judah.seq.track.MidiTools;
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

	@Getter private volatile TrackList<MidiTrack> tracks = new TrackList<>();
	private final TrackList<DrumTrack> drumTracks;
	private final TrackList<ChannelTrack> ccTracks = new TrackList<ChannelTrack>();
	@Getter private final ChannelTrack mains;

	private final JudahZone zone;
	private final Chords chords;
	private final Sampler sampler;
	private final DrumMachine drums;

	private final JPanel dnb = new JPanel(new GridLayout(1, 5, 1, 0));
	private final JPanel synths = new JPanel();
	private final MidiInstrument bass;
	private MidiFile bundle;
	public int getResolution() { return bundle == null ? 0 : bundle.getResolution(); }

	private TraxCombo trax;

	public Seq(JudahZone judahZone) {
		this.zone = judahZone;
		this.chords = zone.getChords();
		this.sampler = zone.getSampler();
		this.drums = zone.getDrumMachine();
		this.bass = zone.getBass();

		drumTracks = drums.getTracks();

		ChannelTrack temp = null;
		try {
			temp = new ChannelTrack(zone.getMains(), 0, zone.getMains().getName());
			byte[] copyright = new String(JudahZone.JUDAHZONE + " " + Year.now().getValue()).getBytes();
			temp.getT().add(meta(Meta.COPYRIGHT, copyright, 0l));
			temp.setPermanent(true);
		} catch (InvalidMidiDataException e) { RTLogger.warn(this, e); }
		mains = temp;

		refill();
		gui();
	}

	public TraxCombo getTrax() {
		if (trax == null)
			trax = new TraxCombo(this);
		return trax;
	}

	public Automation getAutomation() {
		return tracks.getCurrent().getAutomation();
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
		refill();
	}

	public void init(List<Sched> tracks) {
		tracks.clear();
		for (int i = 0; i < numTracks(); i++)
			tracks.add(new Sched(get(i).isSynth()));
	}

	private void clearSynths() {
		for (ZoneMidi engine : SynthRack.engines) {
			Vector<? extends MidiTrack> cars = engine.getTracks();
			for (int i = cars.size() - 1; i >= 0; i--) {
				MidiTrack t = cars.get(i);
				if (!t.isPermanent())
					cars.remove(i);
			}
		}
	}

	/** Load song into sequencer. @return true if SmashHit bundle format */
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
    			t = allocateTrack(info);
    			if (t == null) continue;
    		}
    		t.load(info);
		}
	}

	/** Allocate a new track from TrackInfo metadata */
	private MidiTrack allocateTrack(TrackInfo info) {
		String name = info.getTrack();
		String ch = info.getChannel();
		if (ch != null) {
			Channel channel = zone.getMixer().byName(ch);
			if (channel == null) {
				RTLogger.warn(this, "Missing ChannelTrack: " + ch);
				return null;
			}
			return addTrack(channel, name);
		}
		if (name.equals("B"))
			return bass.getTrack();
		if (name.startsWith("F"))
			return addTrack(name, RegisteredSynths.Fluid, 0);
		if (name.startsWith("T"))
			return addTrack(name, RegisteredSynths.Taco, 1);
		if (info.getFile() == null || info.getFile().isBlank())
			return null;
		RTLogger.warn(this, name + " - Unknown Track: " + info);
		return null;
	}

	public MidiTrack byName(String track) {
		for (MidiTrack t : this)
			if (t.getName().equals(track))
				return t;

		for (Trax legacy : Trax.values())
			if (legacy.name().equals(track) || legacy.getName().equals(track))
				return legacy(legacy);

		return null;
	}

	private MidiTrack legacy(Trax type) {
		return switch(type) {
			case D1 -> drums.getTrack();
			case D2 -> drums.getTracks().get(1);
			case H1 -> drums.getTracks().get(2);
			case H2 -> drums.getTracks().get(3);
			case B -> SynthRack.getOther()[0].getTrack();
			case F1 -> SynthRack.getFluids()[0].getTrack();
			case TK1 -> SynthRack.getTacos()[0].getTrack();
			case TK2 -> SynthRack.getTacos()[1].getTrack();
			default -> null;
		};
	}

	public void step(int step) {
		chords.step(step);
		sampler.step(step);
	}

	/** Real-time audio callback:  JudahMidi -> JudahClock -> here
	 * snapshot tracks before iteration */
	public void percent(float percent) {
		TrackList<MidiTrack> snap = tracks;
		for (int i = 0; i < snap.size(); i++)
			snap.get(i).playTo(percent);
		mains.playTo(percent);
	}

	/** Atomically rebuild track list and trigger UI update */
	void refill() {
		TrackList<MidiTrack> newTracks = new TrackList<>();
		newTracks.addAll(drumTracks);
		newTracks.addAll(SynthRack.getSynthTracks());
		newTracks.addAll(ccTracks);
		this.tracks = newTracks;
		SwingUtilities.invokeLater(this::updates);
	}

	/** Recording/transposing handler. Drum pads sounded from here */
	public boolean captured(Midi midi) {
		boolean result = false;
		if (midi.getChannel() == DRUM_CH) {
			Midi note = translateDrums(midi);
			for (DrumTrack t : drumTracks)
				if (t.capture(note))
					result = true;
			if (!result && Midi.isNoteOn(note))
				drums.getChannel(note.getChannel()).send(note, JudahMidi.ticker());
			return true;
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
		sb.append(SynthRack.getSynthTracks().size()).append(" synths ");
		sb.append(ccTracks.size()).append(" channels.").append(Constants.NL).append(Constants.NL);
		return sb.toString();
	}

	public ChannelTrack addTrack(Channel ch, String trackName) {
		try {
			ChannelTrack result = new ChannelTrack(ch, ccTracks.size(), trackName);
			ccTracks.add(result);
			refill();
			return result;
		} catch (InvalidMidiDataException e) { RTLogger.warn(this, e); }
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
			} else {
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
			} else {
				result = new PianoTrack(name, new Actives(engine, engine.getTracks().size()), chords);
				((MidiInstrument)engine).getTracks().add(result);
			}
			refill();
		} catch (InvalidMidiDataException e) { RTLogger.warn(this, e); }
		return result;
	}

	public void removeTrack(MidiTrack track) {
		for (ZoneMidi zone : SynthRack.getAll()) {
			if (zone.getTracks().contains(track)) {
				zone.getTracks().remove(track);
				refill();
				return;
			}
		}
	}

	private void gui() {
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        Gui.resize(this, SIZE);
		for (DrumTrack t : drumTracks)
    		dnb.add(new TrackButton(t, this));
        add(dnb);
        add(synths);
        update();
        dnb.setBorder(BorderFactory.createLineBorder(Pastels.MY_GRAY));
	}

	private void updates() {
 		PianoTrack bassTrack = bass.getTrack();
 		if (dnb.getComponentCount() == 5 && ((TrackButton)dnb.getComponent(4)).getTrack() != bassTrack) {
 			dnb.remove(4);
 			if (bassTrack != null)
 				dnb.add(new TrackButton(bassTrack, this));
 		} else if (dnb.getComponentCount() < 5 && bassTrack != null) {
 			dnb.add(new TrackButton(bassTrack, this));
 		}

 		synths.removeAll();
 		for (PianoTrack t : SynthRack.getSynthTracks())
 			if (t != bassTrack)
 				synths.add(new TrackButton(t, this));
 		synths.setLayout(new GridLayout(1, synths.getComponentCount()));

 		if (trax != null)
 			trax.refill(getTracks(), getCurrent());

 		zone.getOverview().refill();
 		if (MainFrame.getKnobMode() == KnobMode.Track)
 			((TrackKnobs)MainFrame.getKnobs()).refill(tracks);
 		if (zone.getFrame() != null)
 			zone.getFrame().getMenu().refillTracks();
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

	public void clear(MidiTrack t) {
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

	/** Bundle legacy tracks into MidiFile with metadata */
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

			String engine = "Bass";
			byte port = 0;
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
		if (!smashHit.isFile()) {
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
				switch(device) {
					case "Drums" -> allocateDrum(map);
					case "Taco" -> allocateTaco(map);
					case "Fluid" -> allocateFluid(map);
					case "Bass" -> bass.getTrack().importTrack(map, getResolution());
					default -> allocateChannel(map);
				}
			}
			refill();
		} catch (Exception e) { RTLogger.warn(this, e); }
	}

	void allocateDrum(MetaMap map) {
		int ch = map.getInt(Meta.CHANNEL);
		if (ch >= 0 && ch < drums.getTracks().size())
			drums.getTrack(drums.getChannel(ch)).importTrack(map, getResolution());
	}

	void allocateTaco(MetaMap map) {
		int port = map.getInt(Meta.PORT);
		while (SynthRack.getTacos().length < port)
			SynthRack.makeTaco();
		addTrack(map, SynthRack.getTacos()[port]);
 	}

	void allocateFluid(MetaMap map) {
		int port = map.getInt(Meta.PORT);
		int deficit = port - SynthRack.getFluids().length;
		for (int i = 1; i <= deficit; i++) {
			String name = "F" + i;
			if (i == deficit)
				new FluidAssistant(name, map);
			else
				new FluidAssistant(name);
		}
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
		if (trax != null && trax.getSelectedItem() != getCurrent())
			trax.setSelectedItem(getCurrent());
		repaint();
	}

	public void update(Computer t) {
		for (Component c : dnb.getComponents())
			if (((TrackButton)c).getTrack() == t)
				((TrackButton)c).update();
		for (Component c : synths.getComponents())
			if (((TrackButton)c).getTrack() == t)
				((TrackButton)c).update();
	}

	public void update(Update type, Computer c) {
		if (c instanceof NoteTrack track && track.getTrackKnobs() != null)
			track.getTrackKnobs().update(type);
		if (type == Update.PLAY || type == Update.CAPTURE)
			update(c);
	}

	public boolean confirmBundle() {
		int rez = JudahClock.MIDI_24;
		for (MidiTrack t : tracks)
			if (t.getResolution() > rez)
				rez = t.getResolution();

		String result = JOptionPane.showInputDialog(zone.getFrame(),
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
