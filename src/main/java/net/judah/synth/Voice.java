package net.judah.synth;

import net.judah.util.Constants;

/** An individual piano key press, three oscillators per voice */
public class Voice {
	public static final int OSCILLATORS = 3;
	private final int SAMPLE_RATE = Constants.sampleRate();

	
	private final JudahSynth synth;
	private final int idx;
	
	private Dco[] oscillators = new Dco[OSCILLATORS];

	private final Adsr env;
	private float amplify, attack, decay, sustain, release;
	
	public Voice(int idx, JudahSynth synth) {
		this.idx = idx;
		this.synth = synth;
		env = synth.getEnv();
		for (int i = 0; i < OSCILLATORS; i++)
			oscillators[i] = new Dco();
	}
	
	/**inspired by: https://github.com/michelesr/jack-oscillator/blob/master/src/lib/synth.c
	 * Under the GNU License. Copyright (C) 2014-2015 Michele Sorcinelli */
	public float calcEnv(Polyphony notes) {
		float out;
		if (notes.getNotes()[idx] != null) { // note is down, do either A/D/S
			if (attack < env.attackGain) {
				attack += (env.attackGain / (SAMPLE_RATE * env.attackTime / 1000));
				out = attack; 
			}
			else if ((decay > sustain) && (sustain <= env.attackGain)) {
				decay -= (env.attackGain - env.sustainGain) / (SAMPLE_RATE * env.decayTime / 1000);
				out = decay;
			}
			else {
				out = env.sustainGain;
			}
		} 
		else { // do release;
			if (release > 0) {
				release -= ( (env.sustainGain * 0.9f) / (SAMPLE_RATE * env.releaseTime / 1000));
				out = release;
			}
			else { // note complete
				return 0f;
			}
		}
		return out * 0.5f; // dampen
	}
	
	public void reset(float hz) {
		attack = 0;
		decay = 1f;
		release = 0.1f;
		sustain = env.sustainGain;
		for (Dco osc : oscillators)
			osc.setHz(hz);
	}

	public void process(Polyphony notes, float[] mono) {
		amplify = 0.5f * calcEnv(notes);
		for (int i = 0; i < OSCILLATORS; i++)
			oscillators[i].process(amplify * synth.computeGain(i), synth.getShape(i).getWave(), mono);
	}

	
	
}
