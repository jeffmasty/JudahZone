package net.judah.fx;

import java.nio.FloatBuffer;
import java.security.InvalidParameterException;

import judahzone.api.Effect.RTEffect;
import judahzone.util.Constants;
import lombok.Getter;
import lombok.Setter;

@Getter
public class Gain implements RTEffect {

	public enum Settings {VOLUME, PAN};

	public static final int VOLUME = 0;
	public static final int PAN = 1;

	private final String name = "Gain";
	private final int paramCount = 2;
	private float gain = 0.5f;
	private float stereo = 0.5f;
	@Setter private float preamp = 1f;

	/** pan/balance */
	public boolean isActive() {
		return stereo < 0.49f || stereo > 0.51f;
	}

	/** pan/balance */
	public void setActive(boolean active) {
		if (!active) stereo = 0.5f;
	}

	@Override public int get(int idx) {
		if (idx == VOLUME)
			return (int) (gain * 100);
		if (idx == PAN)
			return (int) (stereo * 100);
		throw new InvalidParameterException("idx " + idx);
	}

	@Override public void set(int idx, int value) {
		if (idx == VOLUME)
			setGain(value * 0.01f);
		else if (idx == PAN)
			setPan(value * 0.01f);
		else throw new InvalidParameterException("idx " + idx);
	}

	public void setGain(float g) {
		gain = g < 0 ? 0 : g > 1 ? 1 : g;
	}
	public void setPan(float p) {
		stereo = p < 0 ? 0 : p > 1 ? 1 : p;
	}

	public float getLeft() {
		if (stereo < 0.5f) // towards left, half log increase
			return (1 + (0.5f - stereo) * 0.2f) * preamp;
		return 2 * (1 - stereo) * preamp;
	}

	public float getRight() {
		if (stereo > 0.5)
			return (1 + (stereo - 0.5f) * 0.2f) * preamp;
		return 2 * stereo * preamp;
	}

	/**
	 * Apply Gain as a single combined preamp(pan) + post(fader) smoothing pass.
	 *
	 * <p>Purpose and semantics
	 * <ul>
	 *   <li>This method performs a single in-place pass over the supplied buffer(s) that
	 *       combines the effect of the preamp/pan stage and the post-fader stage. It
	 *       computes per-channel multiplicative ramps for the "pre" (pan/preamp) targets
	 *       and the "post" (gain/fader) target and applies the product per sample:
	 *       sample *= (preRamp * postRamp).</li>
	 *   <li>It is intended as a convenience / approximation for callers that want to
	 *       apply smoothing for both preamp and fader in one pass. It does NOT reproduce
	 *       the semantic difference between running preamp before the FX chain and post
	 *       after the FX chain: intermediate effects will see the combined gain when
	 *       you call this method, whereas the two-step preamp(...); FX...; post(...);
	 *       model lets effects see only the preamp-smoothed signal.</li>
	 *
	 * <p>Buffer / mono-stereo contract
	 * <ul>
	 *   <li>{@code right == null} signals mono processing. In mono mode this implementation
	 *       uses the left-channel pan target as the preamp target (see note below).</li>
	 *   <li>For stereo processing both {@code left} and {@code right} should be distinct
	 *       FloatBuffers (not the same instance). Passing the same buffer for left and
	 *       right will effectively double-process the same data and is incorrect.</li>
	 *   <li>The routine uses the buffer {@code limit()} to determine the number of frames
	 *       to process (it uses the minimum limit for stereo), so callers should provide
	 *       buffers with the desired limit/size (typically position==0 and limit==N_FRAMES).</li>
	 *
	 * <p>Smoothing and pan law
	 * <ul>
	 *   <li>The method consults the Gain instance's pan/preamp and fader state via
	 *       {@code getLeft()}, {@code getRight()} and the scalar {@code gain} and produces
	 *       linear per-sample ramps from the last-used values (preCurrentL / preCurrentR /
	 *       postCurrent) to the current targets over the buffer length. The class fields
	 *       {@code preCurrentL}, {@code preCurrentR} and {@code postCurrent} are updated
	 *       to the current targets at the end of the call.</li>
	 *   <li>The pan law used comes from {@code getLeft()/getRight()} (the same pan law used
	 *       by the two-step API). Ramping is linear in amplitude across the buffer; if you
	 *       prefer perceptual (dB/exponential) smoothing, replace the linear interpolation
	 *       with an exponential/dB-based curve.</li>
	 *
	 * <p>Limitations and gotchas
	 * <ul>
	 *   <li>This single-pass approach cannot emulate the case where effects between pre and
	 *       post need to see only the preamped signal. If your processing chain contains
	 *       effects that must react to the input level (compressors, distortion, time-based
	 *       effects with level-dependent behavior) keep the separate preamp(...) / FX / post(...)
	 *       calls instead.</li>
	 *   <li>If parameters (pan/preampl/fader) change between buffers this method smooths both
	 *       changes over the current buffer. If you require the FX to see unsmoothed preamp
	 *       values, use the two-step API.</li>
	 *   <li>Mono branch uses the left-channel pan target. If you prefer a true mono pan
	 *       behavior use an appropriate mono pan law (for example averaging left/right targets
	 *       or defining a dedicated mono preamp target).</li>
	 *
	 * <p>Thread-safety
	 * <ul>
	 *   <li>The method mutates instance fields {@code preCurrentL}, {@code preCurrentR} and
	 *       {@code postCurrent}. Callers should ensure access is correctly synchronized if
	 *       these fields may be read or written concurrently from multiple threads. In the
	 *       typical usage (single RT thread calling this per-buffer) no additional sync is
	 *       required.</li>
	 *
	 * <p>Performance
	 * <ul>
	 *   <li>The implementation uses absolute indexing (get(i)/put(i,...)). If the FloatBuffer
	 *       has a backing array (hasArray()), an alternative implementation that operates on
	 *       the float[] backing array will usually be faster and reduce JNI/ByteBuffer overhead.</li>
	 *
	 * <p>Recommended usage
	 * <ul>
	 *   <li>Use this method when you want a compact single-pass smoothing + pan application
	 *       and you do not have or do not care about effects that must see only the preamp
	 *       level. When you need correct effect-facing behavior (effects must be fed the
	 *       preamp-smoothed signal while the fader is applied afterwards), keep the separate
	 *       preamp(...) and post(...) calls.</li>
	 *
	 * @param left  left-channel buffer (must not be null)
	 * @param right right-channel buffer, or null for mono processing
	 */
	@Override
	public void process(FloatBuffer left, FloatBuffer right) {
	    left.rewind();
	    if (right == null) {
	        // Mono: apply combined ramp for preamp * gain
	        float targetPre = getLeft(); // in mono, you can use getLeft() or compute a mono pan-law
	        float targetPost = gain;
	        int n = left.limit();
	        float stepPre = (targetPre - preCurrentL) / n;
	        float stepPost = (targetPost - postCurrent) / n;
	        float curPre = preCurrentL;
	        float curPost = postCurrent;
	        for (int i = 0; i < n; i++) {
	            float m = curPre * curPost;
	            left.put(i, left.get(i) * m);
	            curPre += stepPre;
	            curPost += stepPost;
	        }
	        preCurrentL = targetPre;
	        preCurrentR = targetPre;
	        postCurrent = targetPost;
	        return;
	    }

	    right.rewind();

	    // stereo
	    float targetPreL = getLeft();   // uses pan law * preamp
	    float targetPreR = getRight();
	    float targetPost = gain;

	    int n = Math.min(left.limit(), right.limit());
	    float stepPreL = (targetPreL - preCurrentL) / n;
	    float stepPreR = (targetPreR - preCurrentR) / n;
	    float stepPost = (targetPost - postCurrent) / n;

	    float curPreL = preCurrentL;
	    float curPreR = preCurrentR;
	    float curPost = postCurrent;

	    for (int i = 0; i < n; i++) {
	        float mL = curPreL * curPost;
	        float mR = curPreR * curPost;
	        left.put(i, left.get(i) * mL);
	        right.put(i, right.get(i) * mR);
	        curPreL += stepPreL;
	        curPreR += stepPreR;
	        curPost += stepPost;
	    }

	    preCurrentL = targetPreL;
	    preCurrentR = targetPreR;
	    postCurrent = targetPost;
	}

	public void processMono(FloatBuffer mono) {
		float precompute = preamp * gain;
		for (int z = 0; z < Constants.bufSize(); z++)
			mono.put(mono.get(z) * precompute);
	}

	/** Last effective left/right gains used in preamp() (preamp * pan). */
	private float preCurrentL = 1f;
	private float preCurrentR = 1f;

	/** Last effective post-fader gain used in post(). */
	private float postCurrent = 1f;

	/** preamp and panning, with smoothing, stereo only */
	public void preamp(FloatBuffer left, FloatBuffer right) {
		// target per-channel gains for this buffer (preamp * pan law)
		float targetL = getLeft();
		float targetR = getRight();

		// Stereo: ramp both channels over N_FRAMES
		ramp(left,  N_FRAMES, preCurrentL, targetL);
		ramp(right, N_FRAMES, preCurrentR, targetR);

		preCurrentL = targetL;
		preCurrentR = targetR;
	}

	/** gain only, with smoothing, stereo only */
	public void post(FloatBuffer left, FloatBuffer right) {
		float target = gain;

		ramp(left,  N_FRAMES, postCurrent, target);
		ramp(right, N_FRAMES, postCurrent, target);

		postCurrent = target;
	}

	// apply a linear ramp from start→end over N_FRAMES samples
	private static void ramp(FloatBuffer buf, int frames, float startGain, float endGain) {
		if (frames <= 0) {
			return;
		}
		float step = (endGain - startGain) / frames;
		float g = startGain;

		if (buf.hasArray()) {
			float[] arr = buf.array();
			int base = buf.arrayOffset();
			// Assume samples start at index 0 for this ring buffer; if not, adjust indexing as needed
			for (int i = 0; i < frames; i++) {
				arr[base + i] *= g;
				g += step;
			}
		} else {
			for (int i = 0; i < frames; i++) {
				float s = buf.get(i);
				buf.put(i, s * g);
				g += step;
			}
		}
	}

	@Override
	public void reset() {
		gain = 0.5f;
		stereo = 0.5f;
		preamp = 1f;
		preCurrentL = 1f;
		preCurrentR = 1f;
		postCurrent = 1f;
	}
}