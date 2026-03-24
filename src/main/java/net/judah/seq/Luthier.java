package net.judah.seq;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.sound.midi.InvalidMidiDataException;

import judahzone.api.MidiUpdate;
import judahzone.synth.WavSynth;
import judahzone.util.RTLogger;
import net.judah.channel.Generator;
import net.judah.drums.DrumMachine;
import net.judah.gui.MainFrame;
import net.judah.seq.chords.Chords;
import net.judah.seq.track.NoteTrack;
import net.judah.seq.track.PianoTrack;
import net.judah.seq.track.TrackInfo;
import net.judah.synth.SynthEngine;
import net.judah.synth.fluid.FluidSynth;

/** Minimal factory / registry for creating PianoTrack instances.
    Seq constructs one Luthier on ping\(\) and registers engines as they arrive.
    Luthier is the single place that creates PianoTracks (per repo design). */
public class Luthier {

	public static enum Engine { JackMidi, Fluid, WavSynth, PluckSynth, FMSynth }


    private final List<SynthEngine> engines = Collections.synchronizedList(new ArrayList<>());
    private final Chords chords;
    private final MidiUpdate midiUpdate;
    private final Seq seq;
    private final DrumMachine drums;

    public Luthier(MainFrame ping, Seq seq, Chords chords, DrumMachine drumsMachine, SynthEngine... register) {
        this.midiUpdate = ping;
        this.chords = chords;
        this.seq = seq;
        this.drums = drumsMachine;

        drums.init(ping);


        for (SynthEngine e : register)
        	registerEngine(e);

        seq.setFactory(this);

        ping.getMenu().refillTracks();
    }

    /** Register an engine so the factory can allocate tracks on it. Called from Seq.channelAdded when factory exists. */
    public void registerEngine(SynthEngine engine) {
        if (!engines.contains(engine)) {
            engines.add(engine);
            RTLogger.log(this, "Luthier registered engine: " + engine.getName());
        }
    	if (engine.getTracks().isEmpty()) {
    		PianoTrack first = addTrack(engine.getName(), engine);
    		first.setPermanent(true);
    	}
    }

    /** Unregister engine (called by Seq.channelRemoved). Non-RT. */
    public void unregisterEngine(SynthEngine engine) {
    	for (NoteTrack t : engine.getTracks()) {
    		t.setPermanent(false);
			seq.removeTrack(t);
    	}
        engines.remove(engine);
    }

    public PianoTrack addTrack(TrackInfo info) {
    	String engine = info.getChannel();


    	for (SynthEngine e : engines)
    		if (e.getName().equalsIgnoreCase(engine))
				return addTrack(info.getTrack(), e);

    	RTLogger.warn(this, "No engine found for channel '" + engine + "' to create track '" + info.getTrack() + "'");
    	return null;
	}

    public synchronized PianoTrack addTrack(String name, SynthEngine engine) {
		try {
			PianoTrack p = engine.addTrack(name, midiUpdate, chords);
			seq.include(p);
			return p;
		} catch (InvalidMidiDataException er) {
			RTLogger.warn(this, name + ": " + er.getMessage());
			return null;
		}
    }

//    /** Create a PianoTrack from TrackInfo. Returns the created track or null on failure. */
//    public synchronized PianoTrack createPiano(TrackInfo info) {
//        if (info == null) return null;
//        // prefer explicit device / port hints if present
////        String device = info.getDevice();
//        String channelHint = info.getChannel();
//        String name = info.getTrack();
//        ZoneMidi target = null;
//
////        if (device != null && !device.isBlank()) {
////            for (ZoneMidi e : engines)
////                if (device.equalsIgnoreCase(e.getName()) || device.equalsIgnoreCase(e.getClass().getSimpleName())) {
////                    target = e;
////                    break;
////                }
////        }
//
//        if (target == null && channelHint != null && !channelHint.isBlank()) {
//            for (ZoneMidi e : engines)
//                if (channelHint.equalsIgnoreCase(e.getName())) {
//                    target = e;
//                    break;
//                }
//        }
//
//        if (target == null) {
//            // fallback: first registered engine
//            if (!engines.isEmpty()) target = engines.get(0);
//        }
//
//        if (target == null) {
//            RTLogger.warn(this, "No engine available to create PianoTrack: " + name);
//            return null;
//        }
//
//        PianoTrack created = tryCreateOnEngine(target, name);
//        if (created != null) {
//            RTLogger.log(this, "Created PianoTrack '" + name + "' on engine " + target.getName());
//            return created;
//        }
//
//        RTLogger.warn(this, "Luthier failed to create PianoTrack '" + name + "' on " + target.getName());
//        return null;
//    }
//
//    /** Convenience: create a default PianoTrack for a given MidiInstrument engine. */
//    public synchronized PianoTrack createPiano(MidiInstrument mi) {
//        if (mi == null) return null;
//        String base = mi.getName() == null ? "Piano" : mi.getName() + " 0";
//        // prefer using the engine instance if registered
//        for (ZoneMidi e : engines)
//            if (Objects.equals(e, mi))
//                return tryCreateOnEngine(e, base);
//
//        // not registered yet, attempt direct creation on the provided instance via reflection
//        return tryCreateOnEngine(mi, base);
//    }
//
//    /** Try to create a default PianoTrack on the provided ZoneMidi engine. */
//    public synchronized PianoTrack createDefaultTrack(ZoneMidi engine, String name) {
//        if (engine == null) return null;
//        String candidate = name;
//        if (candidate == null || candidate.isBlank())
//            candidate = engine.getName() == null ? "Piano" : engine.getName() + " 0";
//        PianoTrack created = tryCreateOnEngine(engine, candidate);
//        if (created != null)
//            RTLogger.log(this, "Created default PianoTrack '" + candidate + "' on " + engine.getName());
//        return created;
//    }

    /** Remove a previously created PianoTrack from its owning engine. Non-RT. */
    public synchronized void remove(PianoTrack t) {
        if (t == null) return;
        for (SynthEngine e : new ArrayList<>(engines)) {
            try {
                var tracks = e.getTracks();
                if (tracks != null && tracks.contains(t)) {
                    tracks.remove(t);
                    RTLogger.log(this, "Removed PianoTrack " + t.getName() + " from engine " + e.getName());
                    return;
                }
            } catch (Exception ex) {
                RTLogger.warn(this, "remove: " + ex.getMessage());
            }
        }
    }

//    /* Helper: attempt to create a PianoTrack on the engine by probing common API names.
//       If the invoked method returns a PianoTrack, return it. If it is void, return the
//       last track from engine.getTracks() as the newly created one. */
//
//    private PianoTrack tryCreateOnEngine(Object engine, String name) {
//        if (engine == null) return null;
//
//        // Common method name attempts (ordered)
//        String[] names = { "addTrack", "createTrack", "createPiano", "add", "addPiano", "create" };
//
//        Class<?> cls = engine.getClass();
//        for (String methodName : names) {
//            try {
//                Method m = null;
//                // try single String param
//                try { m = cls.getMethod(methodName, String.class); } catch (NoSuchMethodException ignored) {}
//                // try no-arg variant
//                if (m == null) {
//                    try { m = cls.getMethod(methodName); } catch (NoSuchMethodException ignored) {}
//                }
//                if (m == null) continue;
//
//                Object ret;
//                if (m.getParameterCount() == 1)
//                    ret = m.invoke(engine, name);
//                else
//                    ret = m.invoke(engine);
//
//                if (ret instanceof PianoTrack pt)
//                    return pt;
//
//                // if void or returned something else, try to return last track from engine.getTracks()
//                try {
//                    Method getTracks = cls.getMethod("getTracks");
//                    Object tracksObj = getTracks.invoke(engine);
//                    if (tracksObj instanceof java.util.List<?> list && !list.isEmpty()) {
//                        Object last = list.get(list.size() - 1);
//                        if (last instanceof PianoTrack)
//                            return (PianoTrack) last;
//                    }
//                } catch (NoSuchMethodException ignored) {
//                    // engine doesn't expose getTracks; continue trying other methods
//                }
//            } catch (Exception e) {
//                RTLogger.warn(this, "tryCreateOnEngine(" + cls.getSimpleName() + "." + methodName + "): " + e.getMessage());
//            }
//        }
//        throw new NullPointerException(name + ": " + engine);
//    }

    /** Expose current registered engines (read-only snapshot). */
    public List<SynthEngine> getRegisteredEngines() {
        synchronized (engines) { return new ArrayList<>(engines); }
    }

	public FluidSynth[] getFluids() {
		ArrayList<FluidSynth> result = new ArrayList<FluidSynth>();
		engines.stream().filter(engine -> engine instanceof FluidSynth).forEach(engine -> result.add((FluidSynth)engine));
		return result.toArray(new FluidSynth[result.size()]);
	}
//	public TacoTruck[] getTacos() {
//		ArrayList<TacoTruck> result = new ArrayList<TacoTruck>();
//		engines.stream().filter(engine -> engine instanceof TacoTruck).forEach(engine -> result.add((TacoTruck)engine));
//		return result.toArray(new TacoTruck[result.size()]);
//	}

//	public MidiInstrument[] getOther() {
//		ArrayList<MidiInstrument> result = new ArrayList<MidiInstrument>();
//		engines.stream().filter(engine -> engine instanceof MidiInstrument).forEach(engine -> result.add((MidiInstrument)engine));
//		return result.toArray(new MidiInstrument[result.size()]);
//	}

	public void join(SynthEngine engine, String name) {
		try {
			PianoTrack p = engine.addTrack(name, midiUpdate, chords);
			seq.include(p);
		} catch (InvalidMidiDataException e) {
			RTLogger.warn(this, "join: " + e.getMessage());
		}
	}

	public Generator makeTaco() {
		return new Generator(WavSynth.GENERIC, Generator.GENERIC, midiUpdate, chords);
	}

}