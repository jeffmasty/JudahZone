package net.judah.drums.synth;

import judahzone.util.Phase;
import judahzone.util.SineWave;

/**Sympathetic rattle: fixed-ratio resonator bank for metallic/woody tone.*/
public class Rattle {
	private static final SineWave lookup = new SineWave();

	private final Phase[] phases;
	private final float[] ratios;
	private final float ISR;
	private final float invLength;

	private float envelope = 0f;
	private float invEnvelopeFrames = 0f;

	// Smoothing targets for envelope length to avoid discontinuities
	private volatile float invEnvelopeFramesTarget = 0f;
	private int envelopeSmoothCounter = 0;
	private volatile boolean envelopeChanged = false; // signal that envelope frames changed

	/**
	 * Create rattle with custom ratios (e.g., {3.0f, 3.7f, 4.3f} for snare).
	 * Oversampling is an acceptable configuration here
	 */
	public Rattle(float[] ratios, int sampleRate) {
		this.ratios = ratios;
		this.phases = new Phase[ratios.length];
		for (int i = 0; i < phases.length; i++)
			this.phases[i] = new Phase();
		this.ISR = 1.0f / sampleRate;
		this.invLength = 1.0f / ratios.length;
	}

	/** Reset all phases to zero (e.g., on retrigger for phase coherence). */
	public void resetPhase() {
		for (int i = 0; i < phases.length; i++)
			phases[i].reset();
	}

	/** update */
	public void setEnvelopeFrames(int frames) {
		if (frames > 0) {
			// set smoothing target rather than immediate swap
			this.invEnvelopeFramesTarget = 1.0f / frames;
		} else {
			this.invEnvelopeFramesTarget = 0f;
		}
		// Signal that envelope frames changed; smoothing happens once per change
		envelopeChanged = true;
		// Ensure invEnvelopeFrames is seeded if this is the first call
		if (this.invEnvelopeFrames == 0f) {
			this.invEnvelopeFrames = this.invEnvelopeFramesTarget;
			envelopeChanged = false; // don't smooth on initialization
		}
	}

	/** Start the rattle envelope decay. Called on trigger or parameter change. */
	public void startEnvelope(float initialAmount) {
		this.envelope = initialAmount;
	}

	/**Per-sample rattle: returns sum of ratio'd partials * envelope, wrapped to [0..1).
	 * Assumes envelope already started via startEnvelope() before samples loop starts. */
	public float next(float baseFreq) {
		if (invEnvelopeFrames == 0f)
			return 0f;
		float out = process(baseFreq, envelope, invLength);
		// On first sample after envelope frame change, start smoothing over ~5ms window
		if (envelopeChanged) {
			// frames = 1.0 / invEnvelopeFramesTarget; smooth over ~5ms of those frames
			int frames = Math.max(1, Math.round(1.0f / invEnvelopeFramesTarget));
			envelopeSmoothCounter = Math.max(1, Math.round(0.005f * frames));
			envelopeChanged = false; // only smooth once per parameter change
		}
		// Step invEnvelopeFrames toward target during smoothing window
		if (envelopeSmoothCounter > 0) {
			float step = (invEnvelopeFramesTarget - invEnvelopeFrames) / envelopeSmoothCounter;
			invEnvelopeFrames += step;
			envelopeSmoothCounter--;
		}
		envelope = Math.max(0f, envelope - invEnvelopeFrames);
		return out;
	}

	/**Non-stateful processing variant: caller supplies envelope (env) and scaling (inverseCount).
	 * Advances phases and returns the normalized sum. */
	public float process(float baseFreq, float env, float inverseCount) {
		if (env <= 0f)
			return 0f;

		float sum = 0f;
		for (int r = 0; r < phases.length; r++) {
			float inc = baseFreq * ratios[r] * ISR;
			// Use Phase.next to get blended phase value and advance phase(s).
			float ph = phases[r].next(inc);
			sum += lookup.forPhase(ph) * env;
		}
		return sum * inverseCount;
	}

}
