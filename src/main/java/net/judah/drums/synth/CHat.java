package net.judah.drums.synth;

import java.util.function.Consumer;
import java.util.function.Supplier;

import judahzone.data.Env;
import judahzone.data.Filter;
import judahzone.data.Stage;
import judahzone.fx.op.NoiseGen.Colour;
import judahzone.util.Constants;
import judahzone.util.Phase;
import net.judah.drums.DrumType;
import net.judah.drums.synth.CHat.CHatParams;
import net.judah.drums.synth.DrumOsc.OverSampled;
import net.judah.drums.synth.DrumParams.DrumParam;
import net.judah.drums.synth.DrumParams.Freqs;
import net.judah.drums.synth.DrumParams.Settings;
import net.judah.midi.Actives;

/** Closed hi‑hat: density/brightness/wobble with oversampling. Per-instance one-pole shapes
 * brightness so the instrument can respond to `hz` without reconfiguring the shared NoiseGen */
public class CHat extends DrumOsc implements OverSampled, Consumer<CHatParams>, Supplier<CHatParams> {
	public static record CHatParams(float shimmer, float wobble, float bright) { }

	public static Settings SETTINGS = new Settings(
			new Stage(2.5f, 50), new Env(10, 23),
			new Freqs( 	new Filter(2000, 6f),
						new Filter(3278, 9f),
						new Filter(12500, 9f) ),
			Colour.PINK,
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

	public CHat(Actives actives) {
		super(DrumType.CHat, SETTINGS, actives);
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

	@Override public void set(DrumParam idx, int knob) {
	    float val = knob * 0.01f;
	    if (idx == DrumParam.Param1)
	        setWobble(val);
	    else if (idx == DrumParam.Param2)
	        setShimmer(Constants.inverseLog(knob));
	    else if (idx == DrumParam.Param3)
	        setBright(val);
	}

	@Override public int get(DrumParam idx) {
		if (idx == DrumParam.Param1)
			return (int) (wobble * 100f);
		else if (idx == DrumParam.Param2)
			return Constants.reverseInverseLog(shimmer);
		else if (idx == DrumParam.Param3)
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
		transientFrames = Math.max(1, (attack * SR) / 1000);
		invTransient = 1.0f / transientFrames;

		// wobble LFO rate (Hz) scaled by wobble param; precompute per-micro-sample increment
		final float wobbleRateHz = 6.0f * (0.2f + wobble * 1.8f);
		wobbleIncCached = wobbleRateHz * ISR_OVER;

		dirty = false;
	}

	@Override
	public void trigger(int data2) {
		super.trigger(data2);
		// enable a short blend (~5ms) to avoid clicks on retrigger
		int blendSamples = Math.max(1, Math.round(0.005f * SR_OVER));
		wobbleLFO.trigger(blendSamples);
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
				float wob = 1.0f - wobble * 0.5f + 0.5f * lookup.forPhase(wobblePhase);

				// write micro-sample into oversampled buffer; include density + small gain
				work[idx++] = shaped * trans * wob * shimmer * 0.9f;
			}
		}
	}

	public void resetPhase() {
		wobbleLFO.reset();
	}
}
