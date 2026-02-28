package net.judah.drums.noise;

import java.util.function.Consumer;
import java.util.function.Supplier;

import judahzone.api.Hz;
import judahzone.api.RTNotes;
import judahzone.data.Postage;
import judahzone.filter.Coord;
import judahzone.filter.FilterT;
import judahzone.fx.Gain.GainT;
import judahzone.fx.op.Noise.Colour;
import judahzone.util.Constants;
import judahzone.util.Phase;
import net.judah.drums.Drama.Freqs;
import net.judah.drums.Drum.OverSampled;
import net.judah.drums.DrumSetup;
import net.judah.drums.DrumType;
import net.judah.drums.noise.CHat.CHatParams;

/** Closed hi‑hat: density/brightness/wobble with oversampling. Per-instance one-pole shapes
 * brightness so the instrument can respond to `hz` without reconfiguring the shared NoiseGen */
public class CHat extends NoiseOsc implements OverSampled, Consumer<CHatParams>, Supplier<CHatParams> {
	public static record CHatParams(float shimmer, float wobble, float bright) { }
	public static final float DEFAULT_HZ = 3278f;
	public static DrumSetup SETTINGS = new DrumSetup( DrumType.CHat,
			new GainT(2.5f),
			new Postage(15, 201),
			new Freqs( 	new Coord(2000, 6f),
						new FilterT(DEFAULT_HZ, 9f, 0.5f),
						new Coord(12500, 9f) ),
			new Hz(DEFAULT_HZ),
		new String[] { "Shimmer", "Wobble", "Bright" });

	private float shimmer = 0.3f;
	private float wobble = 0.5f;
	private float bright = 0.7f;

	private final Phase wobbleLFO = new Phase(); // wobble LFO as Phase for retrigger/blending

	private float noiseFiltState = 0f;
	private int transientFrames;
	private float invTransient;
	private float wobbleIncCached;

	/* Fixed alpha for the oversampled one-pole. Chosen empirically; removes
	 * the inaudible hz-based computation and keeps behavior stable. */
	private static final float NOISE_ALPHA = 0.6f;

	public CHat(RTNotes actives) {
		super(DrumType.CHat, SETTINGS, Colour.PINK, actives);
		network(shimmer, 419, 603);
	}

	@Override public void accept(CHatParams t) {
		setWobble(t.wobble());
		setShimmer(t.shimmer());
		setBright(t.bright());
	}

	@Override public CHatParams get() {
		return new CHatParams(wobble, shimmer, bright);
	}

	@Override public void set(int idx, int knob) {
	    float val = knob * 0.01f;
	    if (idx == 0)
	        setWobble(val);
	    else if (idx == 1)
	        setShimmer(Constants.inverseLog(knob));
	    else if (idx == 2)
	        setBright(val);
	}

	@Override public int get(int idx) {
		if (idx == 0)
			return (int) (wobble * 100f);
		else if (idx == 1)
			return Constants.reverseInverseLog(shimmer);
		else if (idx == 2)
			return (int) (bright * 100f);
		return 0;
	}

	public void setWobble(float w) {
		wobble = w;
		dirty = true;
	}

	public void setShimmer(float d) {
		shimmer = d;
		allpass(shimmer);
	}

	public void setBright(float b) {
		bright = b;
		dirty = true;
	}

	private void update() {
		if (!dirty)
			return;
		// transient envelope applies per base frame
		transientFrames = Math.max(1, (env.getAttackSamples()));
		invTransient = 1.0f / transientFrames;

		// wobble LFO rate (Hz) scaled by wobble param; precompute per-micro-sample increment
		final float wobbleRateHz = 6.0f * (0.2f + wobble * 1.8f);
		wobbleIncCached = wobbleRateHz * ISR_OVER;

		dirty = false;
	}


	@Override
	protected void generate() {
		update();

		final int factor = FACTOR;

		int idx = 0;
		for (int i = 0; i < N_FRAMES; i++) {
			// transient density envelope: stronger at attack (per base frame)
			final float trans = i < transientFrames ? 1.0f - i * invTransient : 0f;

			// generate `factor` micro-samples for this base frame
			for (int s = 0; s < factor; s++) {

				float raw = noiseGen.next();

				// one-pole to color noise (bright -> less filtering)
				noiseFiltState = noiseFiltState * NOISE_ALPHA + raw * (1.0f - NOISE_ALPHA);
				float shaped = raw * bright + noiseFiltState * (1.0f - bright);

				// wobble LFO modulates amplitude slightly using Phase.next()
				float wobblePhase = wobbleLFO.next(wobbleIncCached);
				float wob = 1.0f - wobble * 0.5f + 0.5f * Phase.sin(wobblePhase);

				// write micro-sample into oversampled buffer; include density + small gain
				work[idx++] = shaped * trans * wob * shimmer * 0.9f;
			}
		}
	}

	public void resetPhase() {
		wobbleLFO.reset();
	}
}
