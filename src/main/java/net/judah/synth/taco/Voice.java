package net.judah.synth.taco;

import java.nio.FloatBuffer;

import net.judah.util.Constants;

/** An individual piano key press, three oscillators per voice */
public class Voice {
	public static final int OSCILLATORS = 3;
	
	private final TacoSynth synth;
	private int data1;
	private final Dco[] oscillators = new Dco[OSCILLATORS];
	private final JackEnvelope env = new JackEnvelope();
	
	public Voice(TacoSynth synth) {
		this.synth = synth;
		for (int i = 0; i < OSCILLATORS; i++)
			oscillators[i] = new Dco(i, synth);
	}
	
	public void reset(int data1) {
		this.data1 = data1;
		env.reset();
		float hz = Constants.midiToHz(data1);
		for (Dco osc : oscillators)
			osc.setHz(hz);
	}
	
	public void process(Polyphony notes, Adsr adsr, FloatBuffer output) {
		if (data1 == 0)
			return;
		float amplify = env.calcEnv(notes, data1, adsr);
		for (int i = 0; i < OSCILLATORS; i++)
			oscillators[i].process(amplify * synth.computeGain(i), synth.getShape(i).getWave(), output);
	}

	public void bend(float factor) {
		for (Dco osc : oscillators)
			osc.setBend(factor);
	}

}
