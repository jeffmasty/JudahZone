package net.judah.synth.taco;

import judahzone.util.Constants;
import judahzone.util.Phase;
import lombok.RequiredArgsConstructor;

/** Digital Controlled Oscillator with phase accumulation and crossfaded retriggering.
    Inspired by: https://github.com/johncch/MusicSynthesizer/blob/master/src/com/fifthrevision/sound/Osc.java
    Under the MIT license. Copyright (c) 2010, Chong Han Chua, Veronica Borges */
@RequiredArgsConstructor
public class Dco {
	private final int SAMPLE_RATE = Constants.sampleRate() * TacoSynth.OVERSAMPLE;
	private final int BUF_SIZE = Constants.bufSize() * TacoSynth.OVERSAMPLE;
	private final int idx;
	private final TacoSynth synth;

	private float freq;
	private float cyclesPerSample;
	private float bend = 1f;
	private final Phase phase = new Phase();
	private static final int RETRIGGER_BLEND_MS = 5; // Smooth click-free retriggers

	public void detune() {
		if (freq > 0)
			cyclesPerSample = synth.detune(idx, freq) * bend / SAMPLE_RATE;
	}

	public void setHz(float hz) {
		freq = hz;
		if (freq > 0) {
			cyclesPerSample = synth.detune(idx, freq) * bend / SAMPLE_RATE;
			// Crossfade to zero over ~5ms to avoid zipper noise on frequency sweeps
			int blendSamples = (int)(SAMPLE_RATE * RETRIGGER_BLEND_MS / 1000f);
			phase.trigger(blendSamples);
		}
	}

	public void setBend(float amount) {
		bend = amount;
		cyclesPerSample = synth.detune(idx, freq) * bend / SAMPLE_RATE;
	}

	public void process(float amp, float[] wave, float[] output) {
		float scaled, fraction;
		int index;
		for (int i = 0; i < BUF_SIZE; i++) {
			float blendedPhase = phase.next(cyclesPerSample);
			scaled = blendedPhase * Shape.LENGTH;
			fraction = scaled - (int)scaled;
			index = (int)scaled;
			output[i] += amp * (1.0f - fraction) * wave[index & Shape.MASK] +
					amp * fraction * wave[(index + 1) & Shape.MASK];
		}
	}
}
