package net.judah.seq;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Vector;

import javax.sound.midi.InvalidMidiDataException;
import javax.swing.ImageIcon;

import judahzone.util.RTLogger;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.judah.JudahZone;
import net.judah.drumkit.DrumMachine;
import net.judah.fx.Gain;
import net.judah.gui.MainFrame;
import net.judah.midi.Actives;
import net.judah.midi.MidiInstrument;
import net.judah.mixer.LineIn;
import net.judah.seq.track.PianoTrack;
import net.judah.synth.ZoneMidi;
import net.judah.synth.fluid.FluidSynth;
import net.judah.synth.taco.Polyphony;
import net.judah.synth.taco.TacoSynth;
import net.judah.synth.taco.TacoTruck;
import net.judahzone.gui.Icons;

public class SynthRack {

	@RequiredArgsConstructor @Getter
	public static enum RegisteredSynths {
		Taco(TacoTruck.class), Fluid(FluidSynth.class), External(MidiInstrument.class);
		public final Class<? extends ZoneMidi> clazz;
	}
	@RequiredArgsConstructor @Getter
	public static enum RegisteredDrums {
		BeatBox(DrumMachine.class), FluiD(FluidSynth.class), ExDrum(MidiInstrument.class);
		private final Class<? extends ZoneMidi> clazz;
	}

	@Getter static final Vector<ZoneMidi> engines = new Vector<ZoneMidi>();

	public static ZoneMidi[] getAll() {
		return engines.toArray(new ZoneMidi[engines.size()]);
	}

	public static FluidSynth[] getFluids() {
		ArrayList<FluidSynth> result = new ArrayList<FluidSynth>();
		engines.stream().filter(engine -> engine instanceof FluidSynth).forEach(engine -> result.add((FluidSynth)engine));
		return result.toArray(new FluidSynth[result.size()]);
	}
	public static TacoTruck[] getTacos() {
		ArrayList<TacoTruck> result = new ArrayList<TacoTruck>();
		engines.stream().filter(engine -> engine instanceof TacoTruck).forEach(engine -> result.add((TacoTruck)engine));
		return result.toArray(new TacoTruck[result.size()]);
	}

	public static MidiInstrument[] getOther() {
		ArrayList<MidiInstrument> result = new ArrayList<MidiInstrument>();
		engines.stream().filter(engine -> engine instanceof MidiInstrument).forEach(engine -> result.add((MidiInstrument)engine));
		return result.toArray(new MidiInstrument[result.size()]);
	}

	@SuppressWarnings("unchecked")
	public static TrackList<PianoTrack> getSynthTracks() {
		TrackList<PianoTrack> result = new TrackList<PianoTrack>();
		engines.forEach(engine -> result.addAll((Vector<PianoTrack>)engine.getTracks()));
		return result;
	}

	public static ZoneMidi getDevice(String meta) {

		if (meta == null)
			throw new InvalidParameterException("Null device meta");
		if (meta.length() == 2) {
			try {
				int idx = Integer.parseInt(meta.substring(1));
				String device = meta.substring(0, 1);
				if (device == "D")
					return JudahZone.getInstance().getDrumMachine();
				else if (device == "T" && idx < getTacos().length)
						return getTacos()[idx];
				else if (device == "F" && idx < getFluids().length)
						return getFluids()[idx];

			} catch (NumberFormatException e) { RTLogger.log(meta, e.getMessage()); }
		}
		for (ZoneMidi line : engines)
			if (line.getName().equals(meta))
				return line;
		throw new InvalidParameterException("No Instance: " + meta);
	}

	// Before GUI startup
	public static void register(ZoneMidi engine) {
		engines.add(engine);
		try {
			if (engine instanceof TacoTruck truck) {
				TacoSynth taco = new TacoSynth(truck.getName(), truck, new Polyphony(truck, truck.getTracks().size()));
				truck.getTracks().add(taco);
			}
			else {
				Actives a = new Actives(engine, engine.getTracks().size());
				((MidiInstrument)engine).getTracks().add(new PianoTrack(engine.getName(),
						a, JudahZone.getInstance().getChords()));
				//((MidiInstrument)engine).getTracks().add(new PianoTrack(engine.getName(), engine, engine.getTracks().size()));
			}
			engine.getTracks().getLast().setPermanent(true);
			RTLogger.debug(SynthRack.class, "registered " + engine.getClass().getSimpleName() + " " + engine.getName());
		} catch (InvalidMidiDataException e) { RTLogger.warn(engine, e); }
	}

	// After GUI startup
	public static void addEngine(ZoneMidi engine) {
		engines.add(engine);
		JudahZone.getInstance().getMixer().addChannel((LineIn)engine);
		JudahZone.getInstance().getSeq().refill();
		RTLogger.debug(SynthRack.class, engine.getName() + " " + engine.getClass().getSimpleName() + " instance added.");
	}

	public static TacoTruck makeTaco() {
		return makeTaco("T" + (getTacos().length + 1), Icons.get("Waveform.png"));
	}

	public static TacoTruck makeTaco(String name, ImageIcon image) {
		TacoTruck result = new TacoTruck(name, image);
		addEngine(result);
		return result;
	}

	public static void gain(int idx, int data2) {

		if (idx == 0) {
			TacoTruck[] tacos = getTacos();
			if (tacos.length > 1) {
				tacos[1].getGain().set(Gain.VOLUME, data2);
				MainFrame.update(tacos[1]);
			}
		}
		else if (idx == 1) {
			FluidSynth[] fluids = getFluids();
			if (fluids.length > 1) {
				fluids[1].getGain().set(Gain.VOLUME, data2);
				MainFrame.update(fluids[1]);
			}
		}
		else if (idx == 2) {
			FluidSynth[] fluids = getFluids();
			if (fluids.length < 2)
				return;
			if (fluids[1].getTrack() == null)
				return;
			fluids[1].getTrack().setAmp(data2 * .01f);
		}
		else if (idx == 3) {
			FluidSynth[] fluids = getFluids();
			if (fluids.length < 2)
				return;
			if (fluids[1].getTracks().size() < 2)
				return;
			fluids[1].getTracks().get(1).setAmp(data2 * .01f);
		}
	}

}



