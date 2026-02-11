package net.judah.sampler.vocoder;

import java.security.InvalidParameterException;
import java.util.Arrays;

import lombok.Getter;
import net.judah.channel.LineIn;

/**
 * Rakarrack-style channel vocoder with compression and ring modulation. Adapted
 * from RRVocoder.java (Rakarrack). Single-band analysis per modulator sample
 * with peak detection, dynamic compression, and ring-mod blending. Mono
 * modulator, mono carrier, mono output.
 */
public class RakarCoder extends ZoneCoder {


	static final int NUM_BANDS = 32;
	static final float GATE_THRESHOLD = 0.005f;
	static final float DECAY_TIME_MS = 10.0f; // peak detector decay
	static final float COMP_TIME_MS = 50.0f; // compressor att/rel
	static final float COMP_THRESHOLD = 0.25f;
	static final float COMP_RATIO = 0.25f;

	@Getter
	int bands = NUM_BANDS;

	// Parameters
	float q = 60.0f; // Filter Q factor
	float muffle = 0.1f; // Peak detector decay (0-1)
	float ringMod = 0.0f; // Ring mod blend (0=env, 1=raw mod)
	float level = 1.0f; // Analysis output level

	// State
	private final Band[] analysisBands;
	private final Band[] synthesisBands;

	private float alpha, beta; // peak detector coefficients
	private float cAlpha, cBeta; // compressor coefficients
	private float cThresh, cpThresh; // compressor threshold (dynamic)
	private float compPeak, compEnv, oldCompEnv;

	@Getter
	private LineIn carrier;
	private volatile float[] carrierMono;

	// Reusable compression buffer to avoid allocations in audio thread
	private final float[] compressed = new float[BUF_SIZE];

	public RakarCoder(LineIn carrier) {
		setCarrier(carrier);
		analysisBands = new Band[NUM_BANDS];
		synthesisBands = new Band[NUM_BANDS];

		// Initialize peak detector & compressor coefficients
		updateDecayCoefficients();

		// Create filterbank: log-spaced BPF bands 200 Hz - 4 kHz
		for (int i = 0; i < NUM_BANDS; i++) {
			analysisBands[i] = new Band();
			synthesisBands[i] = new Band();
		}
		initializeBands(200.0f, 4000.0f);
	}

	public void setCarrier(LineIn carrier) {
		if (carrier == null || this.carrier == carrier) // NULL = off?
			return;
		if (carrier.getLeft().length != BUF_SIZE)
			throw new InvalidParameterException("Carrier buffer size mismatch");
		this.carrier = carrier;
		this.carrierMono = carrier.getLeft();
	}

	/** Muffle parameter controls peak detector decay time. */
	public void setMuffle(float normValue) {
		if (normValue >= 0.0f && normValue <= 1.0f) {
			this.muffle = normValue;
			updateDecayCoefficients();
		}
	}

	public void setQ(float q) {
		if (q >= 10.0f && q <= 100.0f) {
			this.q = q;
			initializeBands(200.0f, 4000.0f);
		}
	}

	public void setRingMod(float blend) {
		this.ringMod = Math.max(0.0f, Math.min(1.0f, blend));
	}

	/**Process flow (per sample):
	 * 1. Apply compression to modulator (peak detection + envelope follower)
	 * 2. Analysis: compressed modulator → BPF bank → peak detector
	 * 3. Synthesis: carrier → BPF bank → modulate by peak env → sum
	 * 4. Ring-mod blend: interpolate between envelope and raw modulator */
	@Override protected void processImpl(float[] monoIn, float[] monoOut) {
		final int frames = monoIn.length;

		// Safety: if no carrier available, output silence
		final float[] localCarrier = carrierMono;
		if (localCarrier == null) {
			Arrays.fill(monoOut, 0.0f);
			return;
		}

		// --- Stage 1: Compress modulator input ---
		float maxGain = 0.0f;
		final int procFrames = Math.min(frames, compressed.length);
		for (int i = 0; i < procFrames; i++) {
			float sample = monoIn[i]; // preamp?

			// Peak detection with decay
			if (Math.abs(sample) > compPeak)
				compPeak = Math.abs(sample);
			compPeak *= beta;

			// Envelope follower
			compEnv = cBeta * oldCompEnv + cAlpha * compPeak;
			oldCompEnv = compEnv;

			// Dynamic compression
			float tmpGain = 1.0f;
			if (compEnv > cpThresh) {
				float compG = cpThresh + cpThresh * (compEnv - cpThresh) / compEnv;
				cpThresh = cThresh + COMP_RATIO * (compG - cpThresh);
				tmpGain = compG / compEnv;
			}
			if (compEnv < cpThresh)
				cpThresh = compEnv;
			if (cpThresh < cThresh)
				cpThresh = cThresh;

			float out = sample * tmpGain;
			compressed[i] = out;
			if (out > maxGain)
				maxGain = out;
		}
		// zero any remaining compressed frames if input length exceeds buffer
		if (procFrames < frames)
			Arrays.fill(compressed, procFrames, frames, 0.0f);

		// --- Stage 2: Analysis + Synthesis per band ---
		for (int i = 0; i < frames; i++) {
			float out = 0.0f;

			for (int b = 0; b < bands; b++) {
				// Analysis: compressed modulator through BPF
				float analysisSample = analysisBands[b].filter.process(compressed[i]);

				// Peak detector on analysis band
				if (Math.abs(analysisSample) > analysisBands[b].speak)
					analysisBands[b].speak = Math.abs(analysisSample);
				analysisBands[b].speak *= beta;

				// Envelope follower
				analysisBands[b].gain = beta * analysisBands[b].oldGain + alpha * analysisBands[b].speak;
				analysisBands[b].oldGain = analysisBands[b].gain;

				// Apply gate
				if (analysisBands[b].gain < GATE_THRESHOLD)
					analysisBands[b].gain = 0.0f;

				// Ring mod: blend between envelope and raw modulator
				float modEnv = (1.0f - ringMod) * analysisBands[b].gain + ringMod * Math.abs(monoIn[i]);

				// Synthesis: carrier through BPF, modulate by gain
				float carrierSample = (i < localCarrier.length) ? localCarrier[i] : 0.0f;
				float synthesisSample = synthesisBands[b].filter.process(carrierSample);
				out += synthesisSample * modEnv * level;
			}

			monoOut[i] = out;
		}
	}

	public void reset() {
		for (int i = 0; i < bands; i++) {
			analysisBands[i].filter.reset();
			synthesisBands[i].filter.reset();
			analysisBands[i].reset();
			synthesisBands[i].reset();
		}
		compPeak = compEnv = oldCompEnv = 0.0f;
		cpThresh = cThresh;
	}

	private void updateDecayCoefficients() {

		// Peak detector decay: muffle parameter 0-1 -> 10ms..100ms
		float decayMs = DECAY_TIME_MS + muffle * 90.0f;
		// alpha/beta matched to sample-rate form: alpha = SR / (SR + tau_samples)
		alpha = SR / (SR + decayMs * 0.001f * SR);
		beta = 1.0f - alpha;

		// Compressor attack/release: use COMP_TIME_MS (was erroneously using decayMs)
		float compMs = COMP_TIME_MS;
		cAlpha = SR / (SR + compMs * 0.001f * SR);
		cBeta = 1.0f - cAlpha;

		cThresh = COMP_THRESHOLD;
		cpThresh = cThresh;
	}

	/** Initialize log-spaced bandpass filters across range. */
	private void initializeBands(float startFreq, float endFreq) {
		double step = Math.pow(endFreq / startFreq, 1.0 / bands);
		double lastFreq = 0.0;

		for (int b = 0; b < bands; b++) {
			lastFreq = (lastFreq > 0.0) ? lastFreq * step : startFreq;

			// Simple 1st-order BPF (Q-based tuning)
			analysisBands[b].filter = new SimpleBPF((float) lastFreq, q, SR);
			synthesisBands[b].filter = new SimpleBPF((float) lastFreq, q, SR);
			analysisBands[b].reset();
			synthesisBands[b].reset();
		}
	}

	/** Minimal BPF using peaking EQ structure. */
	private static class SimpleBPF {



		private float a0, a1, a2, a3, a4;
		private float x1, x2, y1, y2;

		SimpleBPF(float freq, float q, int sr) {
			// Peaking EQ coefficients (center freq, Q, unity gain)
			float w0 = TWO_PI * freq * ISR;
			float sinw0 = (float) Math.sin(w0); // TODO SineWave lookup ?
			float cosw0 = (float) Math.cos(w0);
			float alpha = sinw0 / (2.0f * q);

			float b0 = alpha;
			float b1 = 0.0f;
			float b2 = -alpha;
			float a0norm = 1.0f + alpha;

			this.a0 = b0 / a0norm;
			this.a1 = b1 / a0norm;
			this.a2 = b2 / a0norm;
			// Fix: store normalized denominator coefficients directly (no sign inversion)
			// expected a1 = -2*cosw0, a2 = 1 - alpha
			this.a3 = (-2.0f * cosw0) / a0norm; // a1 / a0
			this.a4 = (1.0f - alpha) / a0norm; // a2 / a0
			reset();
		}

		float process(float sample) {
			float result = a0 * sample + a1 * x1 + a2 * x2 - a3 * y1 - a4 * y2;
			x2 = x1;
			x1 = sample;
			y2 = y1;
			y1 = result;
			return result;
		}

		void reset() {
			x1 = x2 = y1 = y2 = 0.0f;
		}
	}

	/** Band state: filter, peak detector, envelope follower. */
	private static class Band {
		SimpleBPF filter;
		float speak, gain, oldGain;

		void reset() {
			speak = gain = oldGain = 0.0f;
			if (filter != null)
				filter.reset();
		}
	}

}