package net.judah.synth.taco;

import static net.judah.JudahZone.getBass;
import static net.judah.JudahZone.getFluid;
import static net.judah.JudahZone.getTacos;

import java.util.ArrayList;
import java.util.Vector;

import org.jaudiolibs.jnajack.JackPort;

import net.judah.api.ZoneMidi;
import net.judah.gui.MainFrame;
import net.judah.midi.JudahClock;
import net.judah.midi.MidiInstrument;
import net.judah.omni.Icons;
import net.judah.omni.Threads;
import net.judah.seq.Trax;
import net.judah.seq.track.MidiTrack;
import net.judah.seq.track.PianoTrack;
import net.judah.synth.fluid.FluidSynth;

public class TacoTruck {
	public static final String NAME = "Taco";
	public final TacoSynth taco;
	public final FluidSynth fluid;
	public final ZoneMidi[] mpkRoutes;
	public final ArrayList<TacoSynth> tracks = new ArrayList<TacoSynth>();
	private TacoSynth current;

	public TacoTruck(FluidSynth fluid, MidiInstrument bass, JackPort outL, JackPort outR, JudahClock clock) {
		this.fluid = fluid;
		this.taco = new TacoSynth(NAME, Icons.get("taco.png"), outL, outR, null);
		mpkRoutes = new ZoneMidi[] {taco, fluid, bass};
		tracks.add(new TacoSynth(Trax.TK1.toString(), Icons.get("Synth.png"), outL, outR, clock)); // pianoroll1
		tracks.add(new TacoSynth(Trax.TK2.toString(), Icons.get("Waveform.png"),outL, outR, clock)); // pianoroll2
	}

	public void process() {
		taco.process();
		tracks.forEach(track-> track.process());
	}

	public void rotate() {
		if (current == taco)
			setCurrent(tracks.getFirst());
		else if (current == tracks.getFirst())
			setCurrent(tracks.getLast());
		else
			setCurrent(taco);
	}

	public boolean setCurrent(TacoSynth it) {
		if (current == it)
			return false;
		current = it;
		MainFrame.setFocus(current.getSynthKnobs());
		MainFrame.setFocus(current);
		return true;
	}

	public void init() {
		taco.progChange("FeelGood");

		for(TacoSynth zco : getTacos().tracks) {
			zco.progChange("Drops1");
			zco.getTracks().getFirst().load("8ths");
		}
		getBass().getTracks().getFirst().load("Bass2");
		while (getFluid().getChannels().isEmpty())
			Threads.sleep(50); // allow time for FluidSynth to sync
		Vector<PianoTrack> tracks = fluid.getTracks();
		MidiTrack fluid1 = tracks.get(0);
		fluid1.load("8ths");
		fluid1.getMidiOut().progChange("Strings", fluid1.getCh());
		MidiTrack fluid2 = tracks.get(1);
		fluid2.load("CRDSKNK");
		fluid2.getMidiOut().progChange("Palm Muted Guitar", fluid2.getCh());
		MidiTrack fluid3 = tracks.get(2);
		fluid3.load("Synco");
		fluid3.getMidiOut().progChange("Harp", fluid3.getCh());
	}

}
