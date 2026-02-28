package net.judah.drums.noise;

import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.sound.midi.ShortMessage;

import judahzone.api.Frequency;
import judahzone.api.Hz;
import judahzone.api.RTNotes;
import judahzone.data.Postage;
import judahzone.filter.Coord;
import judahzone.filter.FilterT;
import judahzone.fx.Gain.GainT;
import judahzone.fx.op.Noise.Colour;
import judahzone.fx.op.Schroeder;
import judahzone.fx.op.Wobble;
import judahzone.util.Constants;
import judahzone.util.Phase;
import judahzone.util.Ramp;
import net.judah.drums.Drama.Freqs;
import net.judah.drums.Drum.OverSampled;
import net.judah.drums.DrumSetup;
import net.judah.drums.DrumType;
import net.judah.drums.noise.Ride.RideParams;

/** Ride / Crash cymbal: noise + metallic partials, oversampled. */
public class Ride extends NoiseOsc implements OverSampled, Consumer<RideParams>, Supplier<RideParams> {
	public static record RideParams(float shimmer, float noise, int room) {}
	public static final float DEFAULT_HZ = 3000f;
	public static DrumSetup SETTINGS = new DrumSetup( DrumType.Ride,
			new GainT(0.35f), new Postage(30, 185),
			new Freqs(new Coord(1100, 6f),
				new FilterT(DEFAULT_HZ, 0f, 0.5f),
				new Coord(16000, 1f)),
			new Hz(DEFAULT_HZ),
			new String[] { "Shimmer", "Noise", "Room" });

	private static final float STRIKE = 0.3f;
	private static final float BASE_HZ = 1100f;
	private static final float RATIO_HZ = 1100f;
	private static final float[] RATIOS = new float[] {2.3f, 3.1f, 3.9f, 4.8f, 5.7f, 7.1f};

	private float shimmer = 0.25f;
	private float noise = 0.48f;

	/** Ramp used to smoothly drive all-pass feedback (shimmer) across release/knob changes. */
	private final Ramp shimmerRamp;

	private final Phase[] partials = new Phase[6];
	private final float[] partialsIncrement = new float[6];

	private final Wobble wobble = new Wobble(ISR_OVER);
	private final Phase wobbleLFO = new Phase(OVER_BLEND);

	private float filtHiState = 0f;
	private float filtMidState = 0f;

	public Ride(RTNotes actives) {
		super(DrumType.Ride, SETTINGS, Colour.PINK, actives);
		for (int p = 0; p < partials.length; p++) {
			Phase partial = new Phase(OVER_BLEND);
			env.addPhase(partial);
			partials[p] = partial;
		}
		env.addPhase(wobbleLFO);
		env.addPhase(wobble);

		// create all-pass network with initial shimmer
		network(shimmer, 403, 601);

		// Ramp length equals RELEASE_SAMPLES so release smoothing maps to tail length.
		shimmerRamp = new Ramp(RELEASE_SAMPLES);
		// initialize ramp to current shimmer so first buffer is consistent
		shimmerRamp.reset(shimmer);
	}

	@Override public void accept(RideParams t) {
		setShimmer(t.shimmer());
		setNoise(t.noise());
		setRoom(t.room());
	}

	@Override public RideParams get() {
		return new RideParams(shimmer, noise, room);
	}

	@Override public void set(int idx, int knob) {
		float val = knob * 0.01f;
		if (idx == 0)
			setShimmer(val);
		else if (idx == 1)
			setNoise(val);
		else if (idx == 2)
			setRoom(knob);
	}

	@Override public int get(int idx) {
		if (idx == 0)
			return (int) (shimmer * 100f);
		else if (idx == 1)
			return (int) (noise * 100f);
		else if (idx == 2)
			return room;
		return 0;
	}

	public void setShimmer(float s) {
		shimmer = s;
		// Smooth to new target across subsequent buffers
		shimmerRamp.set(s);
		dirty = true;
	}

	public void setNoise(float s) {
		noise = s;
		dirty = true;
	}

	private float scaleHz(float hz) {
		return Constants.reverseLog(getFundamental(), Frequency.MIN, Frequency.MAX) * 0.01f;
	}

	private void update() {
		if (!dirty)
			return;
	    int transientFrames = Math.max(1, env.getAttackSamples() * FACTOR);
		final float invNoise = 1.0f - noise;
		final float hiCutHz = 5000f + invNoise * 20000f;
		final float midCutHz = 2000f + invNoise * 10000f;
		final float wobbleRateHz = 3.0f * (0.5f + invNoise * 2.0f);

		wobble.update(transientFrames, hiCutHz, midCutHz, wobbleRateHz);

		float freq = scaleHz(getFundamental()) * RATIO_HZ + BASE_HZ;
		for (int p = 0; p < partialsIncrement.length; p++)
			partialsIncrement[p] = freq * RATIOS[p] * ISR_OVER;

		dirty = false;
	}

	@Override
	public void trigger(ShortMessage midi) {
		super.trigger(midi);
		dirty = true; // for wobble

		// immediate shimmer response on trigger
		shimmerRamp.reset(shimmer);
	}

	@Override
	protected void generate() {
		update();

		final int factor = FACTOR;
		final float invNoise = 1.0f - noise;

		float transientFrames = wobble.transientFrames;
		float invTransient = wobble.invTransient;
		float hiAlpha = wobble.hiAlpha;
		float midAlpha = wobble.midAlpha;
		float wobbleInc = wobble.wobbleInc;

		int idx = 0;
		float envelope = env.getLastSample();
		for (int i = 0; i < N_FRAMES; i++) {
			final float trans = i < transientFrames ? 1.0f - i * invTransient : 0f;

			for (int s = 0; s < factor; s++) {
				float raw = noiseGen.next();

				filtHiState = filtHiState * hiAlpha + raw * (1.0f - hiAlpha);
				filtMidState = filtMidState * midAlpha + raw * (1.0f - midAlpha);

				float partialSum = 0f;
				for (int p = 0; p < partials.length; p++)
					partialSum += Phase.sin(partials[p].next(partialsIncrement[p]));
				partialSum *= (0.2f + invNoise * 0.8f) / partials.length;

				float wob = 1.0f - invNoise * 0.25f + 0.1f * Phase.sin(wobbleLFO.next(wobbleInc));

				float noiseBody = filtMidState * 0.6f + filtHiState * 0.9f;
				float out = (noiseBody * noise + partialSum) * (1.0f - STRIKE * 0.5f) + (raw * STRIKE * 0.25f);

				work[idx++] = out * trans * wob * envelope;
			}
		}
	}

	@Override
	protected void post() {
	    // If we've entered release, ensure the shimmer ramp is moving toward 0
		if (!open && tail)
	        shimmerRamp.set(0f);

	    // Advance the shimmer ramp by N_FRAMES samples so the ramp progresses in real samples
	    // across this buffer (no allocations, simple per-sample stepping).
	    for (int i = 0; i < N_FRAMES; i++)
	        shimmerRamp.next();

        float currentShimmer = shimmerRamp.get(); // 0..1 range
        float scaled = currentShimmer * 0.6f;
        for (Schroeder ap : allPass[0]) {
            ap.setFeedback(scaled);
            ap.process(left);
        }
        for (Schroeder ap : allPass[1]) {
            ap.setFeedback(scaled);
            ap.process(right);
        }

	}

}
