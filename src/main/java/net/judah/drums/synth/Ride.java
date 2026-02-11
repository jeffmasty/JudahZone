package net.judah.drums.synth;

import java.util.function.Consumer;
import java.util.function.Supplier;

import judahzone.data.Env;
import judahzone.data.Filter;
import judahzone.data.Frequency;
import judahzone.data.Stage;
import judahzone.fx.op.NoiseGen.Colour;
import judahzone.util.Constants;
import judahzone.util.Phase;
import net.judah.drums.DrumType;
import net.judah.drums.synth.DrumOsc.OverSampled;
import net.judah.drums.synth.DrumParams.DrumParam;
import net.judah.drums.synth.DrumParams.Freqs;
import net.judah.drums.synth.DrumParams.Settings;
import net.judah.drums.synth.Ride.RideParams;
import net.judah.midi.Actives;

/** Ride / Crash cymbal: noise + metallic partials, oversampled. */
public class Ride extends DrumOsc implements OverSampled, Consumer<RideParams>, Supplier<RideParams> {
	public static record RideParams(float shimmer, float noise, int room) {}

	public static Settings SETTINGS = new Settings(
			new Stage(0.35f, 50),
			new Env(20, 93, 10),
		new Freqs(new Filter(1100, 6f),
				new Filter(3000, 0f),
				new Filter(16000, 1f)),
		Colour.PINK,
		new String[] { "Shimmer", "Noise", "Room" });

	private static final float STRIKE = 0.3f;
	private static final float BASE_HZ = 1100f;
	private static final float RATIO_HZ = 1100f;
	private static final float[] RATIOS = new float[] {2.3f, 3.1f, 3.9f, 4.8f, 5.7f, 7.1f};

	private float shimmer = 0.25f;
	private float noise = 0.48f;

	private final Phase wobbleLFO = new Phase();
	private final Phase[] partials = new Phase[6];

	private float filtHiState = 0f;
	private float filtMidState = 0f;
	private int transientFrames;
	private float invTransient;
	private float hiAlpha;
	private float midAlpha;
	private float wobbleIncCached;
	private final float[] partialIncCached = new float[6];

	public Ride(Actives actives) {
		super(DrumType.Ride, SETTINGS, actives);
		for (int p = 0; p < partials.length; p++)
			partials[p] = new Phase();
		network(shimmer, 403, 601);
	}

	@Override public void accept(RideParams t) {
		setShimmer(t.shimmer());
		setNoise(t.noise());
		setRoom(t.room());
	}

	@Override public RideParams get() {
		return new RideParams(shimmer, noise, room);
	}

	@Override public void set(DrumParam idx, int knob) {
		float val = knob * 0.01f;
		if (idx == DrumParam.Param1)
			setShimmer(val);
		else if (idx == DrumParam.Param2)
			setNoise(val);
		else if (idx == DrumParam.Param3)
			setRoom(knob);
	}

	@Override public int get(DrumParam idx) {
		if (idx == DrumParam.Param1)
			return (int) (shimmer * 100f);
		else if (idx == DrumParam.Param2)
			return (int) (noise * 100f);
		else if (idx == DrumParam.Param3)
			return room;
		return 0;
	}

	public void setShimmer(float s) {
		shimmer = s;
		allpass(shimmer);
		dirty = true;
	}

	public void setNoise(float s) {
		noise = s;
		dirty = true;
	}

	private float scaleHz(float hz) {
		return Constants.reverseLog(pitch.getFrequency(), Frequency.MIN, Frequency.MAX) * 0.01f;
	}

	private void update() {
		if (!dirty)
			return;

		transientFrames = Math.max(1, (attack * SR) / 1000);
		invTransient = 1.0f / transientFrames;

		final float invNoise = 1.0f - noise;
		final float hiCutHz = 5000f + invNoise * 20000f;
		final float midCutHz = 2000f + invNoise * 10000f;

		hiAlpha = (float) Math.exp(-2.0 * Math.PI * hiCutHz / SR_OVER);
		midAlpha = (float) Math.exp(-2.0 * Math.PI * midCutHz / SR_OVER);

		final float wobbleRateHz = 3.0f * (0.5f + invNoise * 2.0f);
		wobbleIncCached = wobbleRateHz * ISR_OVER;

		float freq = scaleHz(pitch.getFrequency()) * RATIO_HZ + BASE_HZ;
		for (int p = 0; p < partialIncCached.length; p++)
			partialIncCached[p] = freq * RATIOS[p] * ISR_OVER;

		dirty = false;
	}

	@Override
	public void trigger(int data2) {
		super.trigger(data2);
		int blendSamples = Math.max(1, Math.round(0.005f * SR_OVER));
		wobbleLFO.trigger(blendSamples);
		for (Phase p : partials)
			p.trigger(blendSamples);
	}

	@Override
	protected void generate() {
		update();

		final int factor = FACTOR;
		final float invNoise = 1.0f - noise;

		int idx = 0;
		for (int i = 0; i < N_FRAMES; i++) {
			final float trans = i < transientFrames ? 1.0f - i * invTransient : 0f;
			final float sustainMul = 1.0f + shimmer * 3.0f;

			for (int s = 0; s < factor; s++) {
				float raw = noiseGen.next();

				filtHiState = filtHiState * hiAlpha + raw * (1.0f - hiAlpha);
				filtMidState = filtMidState * midAlpha + raw * (1.0f - midAlpha);

				float partialSum = 0f;
				for (int p = 0; p < partials.length; p++)
					partialSum += lookup.forPhase(partials[p].next(partialIncCached[p]));
				partialSum *= (0.2f + invNoise * 0.8f) / partials.length;

				float wob = 1.0f - invNoise * 0.25f + 0.1f * lookup.forPhase(wobbleLFO.next(wobbleIncCached));

				float noiseBody = filtMidState * 0.6f + filtHiState * 0.9f;
				float out = (noiseBody * noise + partialSum) * (1.0f - STRIKE * 0.5f) + (raw * STRIKE * 0.25f);

				work[idx++] = out * trans * sustainMul * wob * 0.8f;
			}
		}
	}

	public void resetPhase() {
		wobbleLFO.reset();
		for (Phase p : partials)
			p.reset();
	}
}
