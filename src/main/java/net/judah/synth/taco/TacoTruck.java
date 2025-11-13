package net.judah.synth.taco;

import static net.judah.JudahZone.getFluid;

import java.nio.FloatBuffer;
import java.util.Vector;

import net.judah.gui.MainFrame;
import net.judah.gui.knobs.KnobMode;
import net.judah.midi.JudahClock;
import net.judah.midi.Midi;
import net.judah.midi.MidiInstrument;
import net.judah.omni.Icons;
import net.judah.omni.Threads;
import net.judah.seq.Trax;
import net.judah.seq.track.PianoTrack;
import net.judah.synth.fluid.FluidSynth;

/** container for synthesizer instances */
public class TacoTruck {
	public final TacoSynth taco;
	public final TacoSynth tk2;
	public final FluidSynth fluid;
	public final MidiInstrument bass;

	private TacoSynth current;

	public TacoTruck(FluidSynth fluid, MidiInstrument bass, JudahClock clock) {
		this.fluid = fluid;
		this.bass = bass;
		taco = new TacoSynth(Trax.TK1, Icons.get("Waveform.png"), clock);
		tk2 = new TacoSynth(Trax.TK2, Icons.get("Synth.png"), clock);
	}

	public void process(FloatBuffer hot1, FloatBuffer hot2) {
		taco.process(hot1, hot2);
		tk2.process(hot1, hot2);
	}

	public void rotate() {
		if (current == null)
			setCurrent(taco);
		else if (current == taco)
			setCurrent(tk2);
		else if (current == tk2)
			setCurrent(taco);
	}

	public boolean setCurrent(TacoSynth it) {
		if (current == it)
			return false;
		current = it;
		MainFrame.setFocus(current.getKnobs());
		MainFrame.setFocus(current);
		return true;
	}

	public void init() {
		taco.progChange("FeelGood");
		taco.getTracks().getFirst().load(Trax.TK1.getFile());

		tk2.progChange(Trax.TK2.getProgram());
		tk2.getTracks().getFirst().load(Trax.TK2.getFile());

		bass.getTracks().getFirst().load(Trax.B.getFile());
		bass.send(new Midi(JudahClock.MIDI_STOP), 0);

		while (getFluid().getChannels().isEmpty())
			Threads.sleep(50); // allow time for FluidSynth to sync

		Threads.timer(666, ()-> {
			MainFrame.setFocus(KnobMode.MIDI);
			Vector<PianoTrack> fluids = fluid.getTracks();
			for (PianoTrack f : fluids) {
				f.progChange(f.getType().getProgram());
				f.load(f.getType().getFile());
			}});

	}

}
