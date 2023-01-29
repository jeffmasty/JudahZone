package net.judah.synth;

import java.nio.FloatBuffer;

/** An individual piano key press, three oscillators per voice */
public class Voice {
	public static final int OSCILLATORS = 3;
	
	private final JudahSynth synth;
	private final int idx;
	private final Dco[] oscillators = new Dco[OSCILLATORS];
	private final JackEnvelope env = new JackEnvelope();
	
	public Voice(int idx, JudahSynth synth) {
		this.idx = idx;
		this.synth = synth;
		for (int i = 0; i < OSCILLATORS; i++)
			oscillators[i] = new Dco(i, synth);
	}
	
	public void reset(float hz) {
		env.reset();
		for (Dco osc : oscillators)
			osc.setHz(hz);
	}

	public void process(Polyphony notes, Adsr adsr, FloatBuffer output) {
		float amplify = env.calcEnv(notes, idx, adsr);
		for (int i = 0; i < OSCILLATORS; i++)
			oscillators[i].process(amplify * synth.computeGain(i), synth.getShape(i).getWave(), output);
	}

	public void bend(float factor) {
		for (Dco osc : oscillators)
			osc.setBend(factor);
	}

	
	
}
