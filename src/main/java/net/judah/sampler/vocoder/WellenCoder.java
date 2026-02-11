package net.judah.sampler.vocoder;

import judahzone.util.Constants;
import judahzone.util.Filters;
import judahzone.util.Filters.Coeffs;
import lombok.Getter;
import net.judah.channel.LineIn;

/** Channel vocoder with multiband analysis/synthesis.
 *  Mono modulator, mono carrier, mono output.
 *  Adapted from Vocoder.java (Dennis Paul)  https://github.com/dennisppaul/wellen / GNU 3.0 License
 *  Adapted from voclib (Philip Bennefall)  https://github.com/blastbay/voclib / MIT License */
public class WellenCoder extends ZoneCoder {

	static final int SR = Constants.sampleRate();
	static final int MAX_BANDS = 96;
	static final int MAX_FILTERS_PER_BAND = 8;

	// Configuration
	@Getter int bands = 32;
	@Getter int filtersPerBand = 4;

	// Parameters (0-100 mapped to ranges)
	float q = 2.0f;        // Filter Q: user control 15-115 → 0.5-5.0
	float level = 1.0f;    // Analysis band level scaling
	float ring = 0.5f;     // Ring mod blend (0=envelope, 1=raw modulator)
	float reactionTime = 0.03f; // Envelope follower decay in seconds

	// State: analysis/synthesis filterbanks + envelope followers
	private final Band[] analysisBands;
	private final Band[] synthesisBands;
	private final Envelope[] envelopes;

	@Getter private LineIn carrier;
	private float[] carrierMono;

	public WellenCoder(LineIn carrier) {
		setCarrier(carrier);
		analysisBands = new Band[MAX_BANDS];
		synthesisBands = new Band[MAX_BANDS];
		envelopes = new Envelope[MAX_BANDS];

		for (int i = 0; i < MAX_BANDS; i++) {
			analysisBands[i] = new Band(filtersPerBand);
			synthesisBands[i] = new Band(filtersPerBand);
			envelopes[i] = new Envelope();
		}

		initializeFilterbank();
		initializeEnvelopes();
	}

	public void setCarrier(LineIn carrier) {
		if (carrier == null || this.carrier == carrier) return;
		this.carrier = carrier;
		this.carrierMono = carrier.getLeft();
	}

	public void setReactionTime(float seconds) {
		if (seconds >= 0.002f && seconds <= 2.0f) {
			this.reactionTime = seconds;
			initializeEnvelopes();
		}
	}

	public void setQ(float q) {
		if (q >= 0.5f && q <= 5.0f) {
			this.q = q;
			initializeFilterbank();
		}
	}

	/** Process flow (per sample):
	    1. Analysis: modulator → BPF bank → envelope follower → gain
	    2. Synthesis: carrier → BPF bank → multiply by gain → sum */
	@Override
	protected void processImpl(float[] monoIn, float[] monoOut) {
		for (int i = 0; i < monoIn.length; i++) {
			float out = 0.0f;

			// Run bands in parallel, accumulate output
			for (int b = 0; b < bands; b++) {
				// Analysis: modulator through BPF bank
				float analysisSample = monoIn[i];
				for (int f = 0; f < filtersPerBand; f++)
					analysisSample = analysisBands[b].filters[f].process(analysisSample);

				// Envelope follower: track analysis band energy
				float envelope = envelopes[b].tick(analysisSample);

				// Ring mod blend: interpolate envelope vs raw modulator
				float gain = envelope * (1.0f - ring) + Math.abs(monoIn[i]) * ring;

				// Synthesis: carrier through BPF bank, modulate by gain
				float synthesisSample = carrierMono[i];
				for (int f = 0; f < filtersPerBand; f++)
					synthesisSample = synthesisBands[b].filters[f].process(synthesisSample);

				out += synthesisSample * gain * level;
			}

			monoOut[i] = out;
		}
	}

	public void reset() {
		for (int i = 0; i < bands; i++) {
			for (int f = 0; f < filtersPerBand; f++) {
				analysisBands[i].filters[f].reset();
				synthesisBands[i].filters[f].reset();
			}
			envelopes[i].reset();
		}
	}

	private void initializeFilterbank() {
		double minFreq = 80.0;
		double maxFreq = Math.min(SR / 2.0, 12000.0);
		double step = Math.pow(maxFreq / minFreq, 1.0 / bands);
		double lastFreq = 0.0;

		for (int b = 0; b < bands; b++) {
			double priorFreq = lastFreq;
			lastFreq = (lastFreq > 0.0) ? lastFreq * step : minFreq;
			double nextFreq = lastFreq * step;
			double bandwidth = (nextFreq - priorFreq) / lastFreq;

			// Create analysis and synthesis BPF filters for this band
			Coeffs coeffs = Filters.compute(
				Filters.FilterType.LowPass,
				(float)lastFreq, SR, (float)bandwidth, 0.0f,
				Filters.BWQType.Q);
			coeffs.normalize();

			for (int f = 0; f < filtersPerBand; f++) {
				analysisBands[b].filters[f] = new Biquad(coeffs);
				synthesisBands[b].filters[f] = new Biquad(coeffs);
			}
		}
	}

	private void initializeEnvelopes() {
		float coef = (float)Math.pow(0.01, 1.0 / (reactionTime * SR));
		for (int i = 0; i < bands; i++)
			envelopes[i].setCoefficient(coef);
	}

	// Lightweight biquad filter (real-time safe)
	private static class Biquad {
		private float a0, a1, a2, a3, a4;
		private float x1, x2, y1, y2;

		Biquad(Coeffs c) {
			this.a0 = c.b0;
			this.a1 = c.b1;
			this.a2 = c.b2;
			this.a3 = c.a1;
			this.a4 = c.a2;
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

	// Band holds a cascade of biquad filters
	private static class Band {
		Biquad[] filters;

		Band(int filterCount) {
			this.filters = new Biquad[filterCount];
		}
	}

	// Lightweight envelope follower (4-tap leaky integrator)
	private static class Envelope {
		private float coef;
		private final float[] history = new float[4];

		void setCoefficient(float coef) {
			this.coef = coef;
		}

		float tick(float sample) {
			float absInput = Math.abs(sample);
			history[0] = ((1.0f - coef) * absInput) + (coef * history[0]);
			history[1] = ((1.0f - coef) * history[0]) + (coef * history[1]);
			history[2] = ((1.0f - coef) * history[1]) + (coef * history[2]);
			history[3] = ((1.0f - coef) * history[2]) + (coef * history[3]);
			return history[3];
		}

		void reset() {
			history[0] = history[1] = history[2] = history[3] = 0.0f;
		}
	}
}
