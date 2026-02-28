package net.judah.drums.pluck;

import judahzone.prism.PrismRT;
import judahzone.util.Constants;
import judahzone.util.Interpolation;
import judahzone.util.Ramp;

/**
 * Fractional delay line optimized for Karplus‑Strong style plucked-string engines.
 *
 * <p>Overview:
 * This class implements a circular, power‑of‑two buffer with bitmask wrapping and
 * fractional read support for musical pitch control. It is intended as a low‑latency,
 * real‑time safe fractional delay used inside Karplus‑Strong loops: fractional reads
 * provide a continuous mapping from delay length to perceived pitch and avoid
 * coarse detuning artifacts that arise when using integer delays only.</p>
 *
 * <p>Signal flow (per-sample):
 * - compute a fractional read index relative to the current write position
 * - wrap index into the buffer using a bitmask (fast modulo)
 * - gather neighbor samples and run the configured interpolation routine (linear or
 *   cubic/Catmull‑Rom)
 * - run a simple one‑pole smoothing filter on the read sample (feedback damping)
 * - compute a write value as (filtered_read * loopGain) + optional excitation
 * - write and advance the write index</p>
 *
 * <p>API & Parameters:
 * - setDelaySamples(float): set fractional delay length (in samples). Clamped to a
 *   safe range that leaves room for interpolation neighbors (min >= 1, max <= bufLen-4).
 * - setFrequency(float): convenience method converting Hz to fractional samples.
 * - setInterpolation(Interpolation): choose the interpolation algorithm (LINEAR or CUBIC).
 * - reset(): zero the internal buffer and state (on NoteOn) — call on noteOn before injection to
 *   avoid carrying previous energy into a new pluck.</p>
 *
 * <p>Note: pickup/tap is not user-settable in this implementation. Pickup is assumed
 * to be colocated with the loop read (tap = 0) to minimize per-sample cost and avoid
 * volatile state in the audio path.</p>
 *
 * <p>Real‑time and performance considerations:
 * - Bitmask wrapping (power‑of‑two buffer) is used for fast modulo arithmetic.
 * - Keep interpolation style simple in time-critical code (linear cheaper than cubic).
 * - The one‑pole smoothing state (fbFilterState) is internal and updated per sample;
 *   use the fbCut parameter passed into process() to control tonal damping. Typical
 *   fbCut values are small (0.01..0.2) for musically useful damping.</p>
 *
 * @see Interpolation
 */
@PrismRT
public class PluckDelay {

	private static final float SR = Constants.sampleRate();
	private static final float ISR = 1f / SR;
	private static final int BASE_FRAMES = Constants.bufSize(); // preferred working size

	private Interpolation interp = Interpolation.CUBIC;

	/* internal buffer size is next power‑of‑two >= BASE_FRAMES to allow bitmask wrapping */
	private final int bufLen;
	private final float IBF; // inverse-buf length for normalization
	private final int mask;
	private final float[] delayLine;

	// pickup is fixed at 0 (co-located with loop read) for cheapest per-sample handling

	private float delaySamples = 32f; // default minimal delay
	private float fbFilterState = 0f; // one-pole state used as loop damping / smoothing
	private int writeIndex = 0;

	// Ramps to smooth parameter changes in the audio path (reduce discontinuities)
	private static final int PARAM_RAMP_LEN = 64; // samples to smooth over (tweakable)
	private final Ramp delayRamp;

	public PluckDelay() {
		this(Constants.nextPow2(Math.max(1, BASE_FRAMES)));
	}

	public PluckDelay(int powerOfTwoLen) {
		if (!Constants.isPow2(powerOfTwoLen))
			throw new IllegalArgumentException("KSDelay length must be power-of-two");
		this.bufLen = powerOfTwoLen;
		IBF = 1f / bufLen;
		this.mask = bufLen - 1;
		this.delayLine = new float[bufLen];
		// initialise buffer to silence
		for (int i = 0; i < bufLen; i++)
			delayLine[i] = 0f;

		// create ramp and initialize to current parameter value
		this.delayRamp = new Ramp(PARAM_RAMP_LEN);
		this.delayRamp.reset(this.delaySamples);
	}

	/** Set interpolation style (linear or cubic). */
	public void setInterpolation(Interpolation style) {
		this.interp = style;
	}

	/** Set delay length explicitly in fractional samples (clamped) */
	public void setDelaySamples(float samples) {
		// keep a small safety margin for interpolation reads
		final float maxSafe = bufLen - 4; // ensure index+2 safe
		final float minSafe = 1.0f;
		final float clamped = Math.max(minSafe, Math.min(samples, maxSafe));
		// store immediate target (keeps behavior for non-RT inspectors)...
		this.delaySamples = clamped;
		// ...and smooth the audio-thread effective delay via Ramp
		this.delayRamp.set(clamped);
		// dirty? callers may reset() to clear old energy if desired
	}

	/** Set frequency in Hz. Converts to samples (fractional). */
	public void setFrequency(float hz) {
	    setDelaySamples(SR / Math.max(20f, Math.min(hz, SR * 0.4f)));
	}

	/**
	 * Return the instantaneous effective frequency derived from the current
	 * (smoothed) delay used in the audio path.
	 */
	public float getFrequency() {
		// use ramp's current value to report the effective delay in samples
		return delayRamp.get() * ISR;
	}

	/**Reset internal buffer / state (on NoteOn)*/
	public void reset() {
		for (int i = 0; i < bufLen; i++)
			delayLine[i] = 0f;
		writeIndex = 0;
		fbFilterState = 0f;
		// jump ramp to current nominal param to avoid transitions during reset
		this.delayRamp.reset(this.delaySamples);
	}

	/**
	 * Core per-sample processing helper.
	 *
	 * @param excitation is added to the write location when inject == true
	 * @param loopGain   scales feedback written back into the buffer
	 * @param fbCut      is the one‑pole blending factor (0..1). Typical small
	 *                   values (0.01..0.2).
	 * @return the output sample:
	 *         - normally the filtered read sample (feedback path)
	 *         - when `inject` is true, returns the freshly written value (filtered*loopGain + excitation)
	 *           so the initial excitation is audible immediately (prevents initial silence
	 *           while the read pointer catches up). This is an RT-safe, cheap behavior tweak.
	 */
	public float process(float excitation, float loopGain, float fbCut) {
	    // smoothed params for this sample
	    final float currDelay = delayRamp.next();

	    // ---- LOOP FEEDBACK READ (base delay => controls pitch) ----
	    float rBase = writeIndex - currDelay;
	    if (rBase < 0f)
	        rBase += bufLen * (float) Math.ceil((-rBase) * IBF);
	    int i0 = ((int) Math.floor(rBase)) & mask;
	    float frac = rBase - (float) Math.floor(rBase);
	    int im1 = (i0 - 1) & mask;
	    int i1 = (i0 + 1) & mask;
	    int i2 = (i0 + 2) & mask;

	    final float s_m1 = delayLine[im1];
	    final float s0 = delayLine[i0];
	    final float s1 = delayLine[i1];
	    final float s2 = delayLine[i2];
	    final float readBase = interp.interp(s_m1, s0, s1, s2, frac);

	    // one-pole damping in feedback path (affects loop, not output pickup directly)
	    fbFilterState += fbCut * (readBase - fbFilterState);
	    final float filtered = fbFilterState;

	    // compute write value and write into buffer (loop uses filtered read)
	    float writeVal = filtered * loopGain;
	    boolean inject = excitation != 0f;
	    if (inject) // true during excitation, otherwise pure feedback
	        writeVal += excitation;

	    // pickup is colocated with loop read (tap = 0) — reuse readBase to avoid extra work
	    final float pickup = readBase;

	    // blend pickup timbre with the loop output; on inject return freshly written value
	    final float out = inject ? writeVal : (0.5f * filtered + 0.5f * pickup);

	    // write and advance (loop unchanged by pickup)
	    delayLine[writeIndex] = writeVal;
	    writeIndex = (writeIndex + 1) & mask;

	    return out;
	}

}