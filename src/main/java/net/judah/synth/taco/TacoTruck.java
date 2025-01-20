package net.judah.synth.taco;

import static net.judah.JudahZone.getBass;
import static net.judah.JudahZone.getFluid;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Vector;

import net.judah.api.Engine;
import net.judah.api.ZoneMidi;
import net.judah.gui.MainFrame;
import net.judah.midi.JudahClock;
import net.judah.midi.MidiInstrument;
import net.judah.omni.Icons;
import net.judah.omni.Threads;
import net.judah.seq.Trax;
import net.judah.seq.track.PianoTrack;
import net.judah.synth.fluid.FluidSynth;

public class TacoTruck {
	public static final String NAME = "Taco";
	public final TacoSynth taco;
	public final FluidSynth fluid;
	public final ZoneMidi[] mpkRoutes;
	public final ArrayList<TacoSynth> tracks = new ArrayList<TacoSynth>();
	private TacoSynth current;

	public TacoTruck(FluidSynth fluid, MidiInstrument bass, JudahClock clock) {
		this.fluid = fluid;
		this.taco = new TacoSynth(NAME, Icons.get("taco.png"), clock);
		mpkRoutes = new ZoneMidi[] {taco, fluid, bass};
		tracks.add(new TacoSynth(Trax.TK1, Icons.get("Waveform.png"), clock)); // pianoroll1
		tracks.add(new TacoSynth(Trax.TK2, Icons.get("Synth.png"), clock)); // pianoroll2
	}

	public void process(FloatBuffer hot1, FloatBuffer hot2) {
		taco.process(hot1, hot2);
		tracks.forEach(track-> track.process(hot1, hot2));
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

		Engine it = tracks.get(0);
		Trax type = Trax.TK1;
		it.progChange(type.getProgram());
		it.getTracks().getFirst().load(type.getFile());

		it = tracks.get(1);
		type = Trax.TK2;
		it.progChange(type.getProgram());
		it.getTracks().getFirst().load(type.getFile());

		getBass().getTracks().getFirst().load(Trax.B.getFile());

		while (getFluid().getChannels().isEmpty())
			Threads.sleep(50); // allow time for FluidSynth to sync

		Vector<PianoTrack> fluids = fluid.getTracks();
		for (PianoTrack f : fluids) {
			f.progChange(f.getType().getProgram());
			f.load(f.getType().getFile());
		}

	}

}
