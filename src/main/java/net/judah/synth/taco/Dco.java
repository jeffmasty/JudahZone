package net.judah.synth.taco;

import judahzone.data.Shape;
import judahzone.util.Constants;
import judahzone.util.Phase;
import lombok.RequiredArgsConstructor;

/** Digital Controlled Oscillator with phase accumulation and cross-fade re-triggering.
    Inspired by: Chong Han Chua, Veronica Borges (c) 2010 (MIT License)
    https://github.com/johncch/MusicSynthesizer/blob/master/src/com/fifthrevision/sound/Osc.java */

@RequiredArgsConstructor
public class Dco {
	private final int SAMPLE_RATE = Constants.sampleRate() * TacoSynth.OVERSAMPLE;
	private final int BUF_SIZE = Constants.bufSize() * TacoSynth.OVERSAMPLE;
	private final int idx;
	private final TacoSynth synth;

	private float freq;
	private float cyclesPerSample;
	private float bend = 1f;

	// Crossfade to zero over ~5ms to avoid zipper noise on frequency sweeps
	private int blendSamples = (int) (SAMPLE_RATE * RETRIGGER_BLEND_MS / 1000f);
	private final Phase phase = new Phase(blendSamples);
	private static final int RETRIGGER_BLEND_MS = 5; // Smooth click-free retriggers

	public void detune() {
		if (freq > 0)
			cyclesPerSample = synth.detune(idx, freq) * bend / SAMPLE_RATE;
	}

	public void setHz(float hz) {
		freq = hz;
		if (freq > 0) {
			cyclesPerSample = synth.detune(idx, freq) * bend / SAMPLE_RATE;
			phase.trigger();
		}
	}

	public void setBend(float amount) {
		bend = amount;
		cyclesPerSample = synth.detune(idx, freq) * bend / SAMPLE_RATE;
	}

	public void process(float amp, float[] wave, float[] output) {
		int mask = Shape.MASK;
		int len = Shape.LENGTH;
		for (int i = 0; i < BUF_SIZE; i++) {
			float blendedPhase = phase.next(cyclesPerSample); // normalized 0..1
			float idxf = blendedPhase * len;
			int index = ((int) idxf) & mask;
			float frac = idxf - (int) idxf;
			float a = wave[index];
			float b = wave[(index + 1) & mask];
			// linear interp: a + frac * (b - a)
			output[i] += amp * (a + frac * (b - a));
		}
	}

}