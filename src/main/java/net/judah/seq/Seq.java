package net.judah.seq;

import static judahzone.api.Midi.DRUM_CH;
import static net.judah.seq.track.MidiTools.meta;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.io.File;
import java.time.Year;
import java.util.ArrayList;
import java.util.HashSet;
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
import judahzone.api.MidiOut;
import judahzone.fx.Gain;
import judahzone.gui.Gui;
import judahzone.gui.Pastels;
import judahzone.gui.Updateable;
import judahzone.util.Folders;
import judahzone.util.RTLogger;
import lombok.Getter;
import net.judah.JudahZone;
import net.judah.channel.Channel;
import net.judah.channel.Generator;
import net.judah.channel.LineIn;
import net.judah.drums.DrumMachine;
import net.judah.gui.MainFrame;
import net.judah.gui.Size;
import net.judah.gui.knobs.KnobMode;
import net.judah.gui.knobs.TrackKnobs;
import net.judah.midi.JudahClock;
import net.judah.midi.JudahMidi;
import net.judah.midi.MidiInstrument;
import net.judah.mixer.Channels;
import net.judah.mixer.Channels.MixBus;
import net.judah.sampler.Sampler;
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
import net.judah.synth.SynthEngine;
import net.judah.synth.fluid.FluidAssistant;
import net.judah.synth.fluid.FluidSynth;

/** Midi Sequencer, MidiTracks container for song */
public class Seq extends Gui.Opaque implements Updateable, Iterable<MidiTrack>, MixBus {
    private static final Dimension SIZE = new Dimension(Size.WIDTH_KNOBS / 2 , 73);

    @Getter private volatile TrackList<MidiTrack> tracks = new TrackList<>();
    private final TrackList<ChannelTrack> ccTracks = new TrackList<ChannelTrack>();
    @Getter private ChannelTrack mains;

    private final JudahZone zone;
    private final Chords chords;
    private final Sampler sampler;
    private final DrumMachine drums;
    private final Channels channels;
    private Luthier factory;
    // pending TrackInfo to allocate once Luthier (factory) is available
    private final List<TrackInfo> pendingTrackInfos = new ArrayList<>();

    private final JPanel dnb = new JPanel(new GridLayout(1, 5, 1, 0));
    private final JPanel synths = new JPanel();
    private MidiFile bundle;
    public int getResolution() { return bundle == null ? 0 : bundle.getResolution(); }

    private TraxCombo trax;

    public Seq(JudahZone judahZone) {
        this.zone = judahZone;
        this.chords = zone.getChords();
        this.sampler = zone.getSampler();
        this.drums = zone.getDrumMachine();
        this.channels = zone.getChannels();

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        Gui.resize(this, SIZE);

    }

    public void gui() {
        dnb.setBorder(BorderFactory.createLineBorder(Pastels.MY_GRAY));

        channels.subscribe(this);
        try {
            mains = new ChannelTrack(zone.getMains(), 0, zone.getMains().getName());
            byte[] copyright = new String(JudahZone.JUDAHZONE + " " + Year.now().getValue()).getBytes();
            mains.getT().add(meta(Meta.COPYRIGHT, copyright, 0l));
            mains.setPermanent(true);
        } catch (InvalidMidiDataException e) { RTLogger.warn(this, e); }


    }

    public void setFactory(Luthier luthier) {
        this.factory = luthier;
        // build drum buttons UI
        for (DrumTrack t : drums.getTracks())
            dnb.add(new TrackButton(t, this));
        add(dnb);

        for (SynthEngine engine: luthier.getRegisteredEngines()) {
            for (NoteTrack t : engine.getTracks())

                if (t instanceof PianoTrack pt) {
                    synths.add(new TrackButton(pt, this));
                }
        }

        // synths panel placeholder (will be filled in updates())
        add(synths);
        reordered();
        // process any queued TrackInfo (created earlier before factory existed)
        update();

        // process any pending TrackInfo we queued before Luthier was available
        if (!pendingTrackInfos.isEmpty()) {
            RTLogger.log(this, "Processing " + pendingTrackInfos.size() + " queued TrackInfos");
            List<TrackInfo> copy = new ArrayList<>(pendingTrackInfos);
            pendingTrackInfos.clear();
            for (TrackInfo info : copy) {
                String name = info.getTrack();
                MidiTrack t = byName(name);
                if (t == null) {
                    t = factory.addTrack(info);
                    if (t == null) continue;
                }
                t.load(info);
            }
        }
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
        reordered();
    }

    public void init(List<Sched> tracks) {
        tracks.clear();
        for (int i = 0; i < numTracks(); i++)
            tracks.add(new Sched(get(i).isSynth()));
    }

    private void clearSynths() {
        for (SynthEngine engine : factory.getRegisteredEngines()) {
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
            reconcileTracks(song.getTracks());
        else
            loadBundle(song.getBundle());
        return !standalone;
    }


    // TODO
    /**
     * Reconcile currently loaded tracks with the list of TrackInfo from a Song.
     * Re-uses existing tracks where possible, allocates missing ones, and
     * removes non-permanent leftovers. Keeps matching logic simple and predictable:
     * 1) If TrackInfo names a channel, prefer existing track bound to that channel.
     * 2) Otherwise try to match by track name.
     */
    public void reconcileTracks(List<TrackInfo> infos) {
        List<MidiTrack> keep = new ArrayList<>();
        // first pass: reuse or allocate
        for (TrackInfo info : infos) {
            MidiTrack existing = findMatchingTrack(info);
            if (existing != null) {
                existing.load(info);
                keep.add(existing);
                continue;
            }
            MidiTrack allocated = factory.addTrack(info); // may be null
            if (allocated != null) {
                // allocateTrack already loads info (allocateTrack uses addTrack/adaptChannel)
                allocated.load(info);
                keep.add(allocated);
            }
        }

        // second pass: remove leftover non-permanent tracks from engines
        // convert keep to a set for O(1) containment checks
        HashSet<MidiTrack> keepSet = new HashSet<>(keep);
        for (SynthEngine engine : new ArrayList<>(factory.getRegisteredEngines())) {
            var engineTracks = engine.getTracks();
            for (int i = engineTracks.size() - 1; i >= 0; i--) {
                MidiTrack t = engineTracks.get(i);
                if (!t.isPermanent() && !keepSet.contains(t))
                    engineTracks.remove(i);
            }
        }

        // cleanup CC tracks (automation / channel tracks)
        for (int i = ccTracks.size() - 1; i >= 0; i--) {
            MidiTrack t = ccTracks.get(i);
            if (!t.isPermanent() && !keepSet.contains(t))
                ccTracks.remove(i);
        }

        // Optional: don't remove drumTracks here unless TrackInfo explicitly references them.
        // Rebuild the sequencing list and update UI
        reordered();
    }

    public MidiTrack byName(String track) {
        for (MidiTrack t : this)
            if (t.getName().equals(track))
                return t;
        return null;
    }

    public void step(int step) {
        chords.step(step);
        sampler.step(step);
    }

    /** Real-time audio callback:  JudahMidi -> JudahClock -> here
     * snapshot tracks before iteration */
    public void process(float percent) {
        TrackList<MidiTrack> snap = tracks;
        for (int i = 0; i < snap.size(); i++)
            snap.get(i).playTo(percent);
        mains.playTo(percent);
    }

    /** Atomically rebuild track list and trigger UI update */
    @Override
    public void reordered() {
        TrackList<MidiTrack> newTracks = new TrackList<>();
        newTracks.addAll(drums.getTracks());
        newTracks.addAll(collectSynthTracks());
        newTracks.addAll(ccTracks);
        this.tracks = newTracks;
        SwingUtilities.invokeLater(this::updates);
    }

    /** Recording/transposing handler. Drum pads sounded from here */
    public boolean captured(Midi midi) {
        boolean result = false;

        if (midi.getChannel() == DRUM_CH) {
            for (DrumTrack t : drums.getTracks())
                if (t.capture(midi))
                    result = true;
            if (!result && Midi.isNoteOn(midi))
                drums.getChannel(midi.getChannel()).send(midi, JudahMidi.ticker());
            return true;
        }

        // Prefer operating on the stable sequencing snapshot first to avoid concurrent
        // mutations while iterating. Capture should avoid unnecessary allocations; only
        // copy the Midi when an MPK handler is present and requires its own instance.
        TrackList<MidiTrack> snap = tracks;
        for (int i = 0; i < snap.size(); i++) {
            MidiTrack t = snap.get(i);
            if (!t.isSynth())
                continue;
            PianoTrack nt = (PianoTrack) t;
            if (t.capture(midi)) {
                result = true;
                break;
            } else if (nt.isMpkOn()) {
                nt.mpk(Midi.copy(midi));
                result = true;
            }
        }

        // Some MidiInstrument-backed synth tracks may not be present in the main
        // tracks snapshot (channel-backed). Iterate the collected synths once and
        // only perform copies when necessary.
        for (PianoTrack t: collectSynthTracks()) {
            if (t.capture(midi))
                result = true;
            else if (t.isMpkOn()) {
                t.mpk(Midi.copy(midi));
                result = true;
            }
        }
        return result;
    }

    public void trigger(int trackNum) {
        if (numTracks() > trackNum)
            get(trackNum).trigger();
    }

    public ChannelTrack adaptChannel(Channel ch, String trackName) {
        try {
            ChannelTrack result = new ChannelTrack(ch, ccTracks.size(), trackName);
            ccTracks.add(result);
            reordered();
            return result;
        } catch (InvalidMidiDataException e) { RTLogger.warn(this, e); }
        return null;
    }

    public void removeTrack(MidiTrack track) {
        for (SynthEngine zone : factory.getRegisteredEngines()) {
            if (zone.getTracks().contains(track)) {
                zone.getTracks().remove(track);
                SwingUtilities.invokeLater(() -> reordered());
                return;
            }
        }
    }

    private void updates() {
        MidiTrack bassTrack = channels.getBass() == null ? null : channels.getBass().getTrack();
        //PianoTrack bassTrack = bass.getTrack();
        if (dnb.getComponentCount() == 5 && ((TrackButton)dnb.getComponent(4)).getTrack() != bassTrack) {
            dnb.remove(4);
            if (bassTrack != null)
                dnb.add(new TrackButton(bassTrack, this));
        } else if (dnb.getComponentCount() < 5 && bassTrack != null) {
            dnb.add(new TrackButton(bassTrack, this));
        }

        synths.removeAll();
        for (PianoTrack t : collectSynthTracks())
            if (t != bassTrack)
                synths.add(new TrackButton(t, this));
        synths.setLayout(new GridLayout(1, synths.getComponentCount()));

        if (zone.getOverview() != null)
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

    /** Bundle legacy tracks into MidiFile with metadata
     * @Param song track name of the first/meta track
     * @return MidiFile containing the bundled tracks and metadata of current sequencer state
     * @throws InvalidMidiDataException if an error occurs during MIDI data processing */
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
            }
//            else if (legacy instanceof TacoSynth t) {
//                engine = Channels.RegisteredSynths.Taco.name();
//                TacoTruck[] trucks = factory.getTacos();
//                for (byte i = 0; i < trucks.length; i++)
//                    if (trucks[i].getTracks().contains(t)) {
//                        port = i;
//                        break;
//                    }
//            }
            else if (legacy instanceof PianoTrack p) {
                FluidSynth[] fluids = factory.getFluids();
                for (byte i = 0; i < fluids.length; i++)
                    if (fluids[i].getTracks().contains(p)) {
                        engine = Channels.RegisteredSynths.Fluid.name();
                        port = i;
                        break;
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
                    device = "Taco";
                switch(device) {
                    case "Drums" -> allocateDrum(map);
                    case "Taco" -> factory.makeTaco();
                    case "Fluid" -> allocateFluid(map);
                    case "Bass" -> {
                        if (channels.getBass() != null)
                        	channels.getBass().getTrack().importTrack(map, getResolution());
                        else
                            factory.makeTaco();
                    }
                    default -> allocateChannel(map);
                }
            }
            reordered();
        } catch (Exception e) { RTLogger.warn(this, e); }
    }

    void allocateDrum(MetaMap map) {
        int ch = map.getInt(Meta.CHANNEL);
        if (ch >= 0 && ch < drums.getTracks().size())
            drums.getTrack(drums.getChannel(ch)).importTrack(map, getResolution());
    }

//    void allocateTaco(MetaMap map) {
//
//    	Generator g = factory.makeTaco();
//    	zone.getChannels().accept(g);

//        int port = map.getInt(Meta.PORT);
//        TacoTruck[] tacos = factory.getTacos();
//        while (tacos.length < port) { // make stubs - janky
//            int idx = tacos.length + 1;
//            zone.getChannels().accept(new TacoTruck("T + " + idx));
//        }
//        // addTrack(map, tacos[port]);
//     }

    void allocateFluid(MetaMap map) {
        int port = map.getInt(Meta.PORT);
        int deficit = port - factory.getFluids().length; // ?
        for (int i = 1; i <= deficit; i++) {
            new FluidAssistant(zone.getChannels(), i);
        }
    }

    void allocateChannel(MetaMap map) {
        String channel = map.getString(Meta.INSTRUMENT);
        if (channel == null || channel.isBlank())
            return;

        // Check for automation channel first
        Channel fx = zone.getChannels().byName(channel);
        if (fx != null && !(fx instanceof MidiOut)) {
            String name = map.getString(Meta.TRACK_NAME);
            if (name == null || name.isBlank())
                name = channel;
            adaptChannel(fx, name);
            return;
        }

        // check engine first (might extend MidiInstrument)
        if (fx instanceof SynthEngine engine) {
            int ch = map.getInt(Meta.CHANNEL);
            for (MidiTrack t : engine.getTracks()) {
                if (t.getCh() == ch) {
                    tracks.add(t);
                    return;
                }
            }
        }
        else if (fx instanceof MidiInstrument midi) {
            tracks.add(midi.getTrack());
            return;
        }

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

    public ArrayList<PianoTrack> getPianoTracks() {
		ArrayList<PianoTrack> result = new ArrayList<>();
		for (MidiTrack t : tracks)
			if (t instanceof PianoTrack pt)
				result.add(pt);
		return result;
	}

    public TrackList<PianoTrack> collectSynthTracks() {
        TrackList<PianoTrack> result = new TrackList<>();
        // collect engine tracks first (preserves engine ordering)
        if (factory != null) {
            for (SynthEngine engine : factory.getRegisteredEngines()) {
                Vector<? extends MidiTrack> engineTracks = engine.getTracks();
                for (MidiTrack mt : engineTracks)
                    if (mt instanceof PianoTrack pt)
                        result.add(pt);
            }
        }

        for (LineIn i : channels.getAudio()) {
			if (i instanceof Generator g) {
				PianoTrack t = g.getTrack();
				if (t != null)
					result.add(t);
			}
		}


        // add channel-backed MidiInstrument tracks, skipping any already included
        HashSet<MidiTrack> seen = new HashSet<>(result.size() * 2 + 1);
        for (MidiTrack mt : result)
            seen.add(mt);

        for (LineIn ch : channels.getInputs()) {
            if (ch instanceof MidiInstrument mi && mi.getTrack() != null && !seen.contains(mi.getTrack())) {
                result.add(mi.getTrack());
                seen.add(mi.getTrack());
            }
        }
        return result;
    }

    // midi-controller knobs
    public void gain(int idx, int data2) {

//        if (idx == 0) {
//            TacoTruck[] tacos = factory.getTacos();
//            if (tacos.length > 1) {
//                tacos[1].getGain().set(Gain.VOLUME, data2);
//                MainFrame.update(tacos[1]);
//            }
//        }
//         else
        	if (idx == 1) {
            FluidSynth[] fluids = factory.getFluids();
            if (fluids.length > 1) {
                fluids[1].getGain().set(Gain.VOLUME, data2);
                MainFrame.update(fluids[1]);
            }
        }
        else if (idx == 2) {
            FluidSynth[] fluids = factory.getFluids();
            if (fluids.length < 2)
                return;
            if (fluids[1].getTrack() == null)
                return;
            fluids[1].getTrack().setAmp(data2 * .01f);
        }
        else if (idx == 3) {
            FluidSynth[] fluids = factory.getFluids();
            if (fluids.length < 2)
                return;
            if (fluids[1].getTracks().size() < 2)
                return;
            fluids[1].getTracks().get(1).setAmp(data2 * .01f);
        }
    }

    @Override
    public void channelAdded(LineIn ch) {
        if (factory == null) // not wired yet
            return; // factory will hopefully connect

        if (ch instanceof MidiInstrument midi) {
            if (midi.getTrack() != null) {
                tracks.add(midi.getTrack());
                reordered();
            }
        }

        if (ch instanceof DrumMachine)
            return;
        if (ch instanceof SynthEngine midi) {
        	factory.registerEngine(midi);
        	for (NoteTrack t : midi.getTracks())
        		tracks.add(t);
			reordered();
        }
        if (ch instanceof Generator synth) {
        	tracks.add(synth.getTrack());
        	reordered();
        }

    }

    @Override public void channelRemoved(LineIn ch) {
        if (ch instanceof SynthEngine midi) {
            for (MidiTrack t : tracks)
                if (t.getChannel() == midi)
                    removeTrack(t);
            reordered();
        }
    }

    @Override
    public void update(LineIn ch) { // visibility/filter/icon/preamp does not apply to tracks
    }

    /** Helper: find an existing MidiTrack that best matches TrackInfo */
    private MidiTrack findMatchingTrack(TrackInfo info) {
        String chName = info.getChannel();
        String trackName = info.getTrack();

        // Prefer explicit channel match
        if (chName != null && !chName.isBlank()) {
            Channel channel = zone.getChannels().byName(chName);
            if (channel != null) {
                for (MidiTrack t : this) {
                    try {
                        Channel tc = t.getChannel();
                        if (tc != null && tc.getName().equals(channel.getName()))
                            return t;
                    } catch (Exception ignored) {}
                }
            }
        }

        // Fallback: exact name match
        if (trackName != null && !trackName.isBlank()) {
            for (MidiTrack t : this)
                if (trackName.equals(t.getName()))
                    return t;
        }

        return null;
    }

    public void include(PianoTrack p) {
        if (p == null) return;
        try {
            // avoid duplicates
            if (tracks != null && !tracks.contains(p)) {
                tracks.add(p);
                // ensure UI/state is updated
                reordered();
            } else {
                // if tracks is null something else is wrong; log and return
                RTLogger.warn(this, "include: tracks list null or already contains track " + (p.getName()));
            }
        } catch (Exception e) {
            RTLogger.warn(this, "include: " + e.getMessage());
        }
    }

}