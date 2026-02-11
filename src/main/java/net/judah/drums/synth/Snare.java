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
import net.judah.drums.synth.DrumParams.DrumParam;
import net.judah.drums.synth.DrumParams.Freqs;
import net.judah.drums.synth.DrumParams.Settings;
import net.judah.drums.synth.Snare.SnareParams;
import net.judah.midi.Actives;

/** Snare: noise + pitched tone + sympathetic rattle. */
public class Snare extends DrumOsc implements Consumer<SnareParams>, Supplier<SnareParams> {
	public static record SnareParams(Colour colour, float mix, float rattle) { }

	public static Settings SETTINGS = new Settings(
			new Stage(0.55f, 50),
			new Env(5, 14),
			new Freqs(	new Filter(180, 6f),
						new Filter(391, 0f),
						new Filter(10000, 6f)),
			Colour.WHITE,
			new String[] { "Color", "Mix", "Rattle" });

	private static final float RATTLE_RATIO = 2.4f; // std multiplier to the Rattle output
	private final float noise = 0.5f; // de-parameterized noise level (0..1, where 1 is longest)
	private final float brightness = 0.6f; // de-parameterized brightness for rattle length (0..1, where 1 is longest)

	private static final float[] RATTLE_RATIOS = new float[] { 3.0f, 3.7f, 4.3f };
	private final Rattle snares = new Rattle(RATTLE_RATIOS, SR);
	private float rattle = 0.2f;
	private float mix = 0.45f;
	private float noiseFiltState = 0f;

	private final Phase body = new Phase();

	private int atkFrames;
	private int dkFrames;
	private int decayStart;
	private int noiseFrames;
	private float invNoiseFrames;
	private float noiseAlpha;

	public Snare(Actives actives) {
		super(DrumType.Snare, SETTINGS, actives);
		tonal = 0.5f;
		atonal = 2f;
	}

	@Override public void accept(SnareParams t) {
		noiseGen.setColor(t.colour());
		setMix(t.mix());
		setRattle(t.rattle());
	}

	@Override public SnareParams get() {
		return new SnareParams(noiseGen.getColour(), mix, rattle);
	}

	@Override public void set(DrumParam idx, int knob) {
		float val = knob * 0.01f;
		if (idx == DrumParam.Param1)
			setNoise(knob);
		else if (idx == DrumParam.Param2)
			setMix(val);
		else if (idx == DrumParam.Param3)
			setRattle(val);
	}

	@Override public int get(DrumParam idx) {
		if (idx == DrumParam.Param1)
			return (noiseGen.getColour().ordinal() * 100) / (Colour.values().length - 1);
		else if (idx == DrumParam.Param2)
			return (int) (mix * 100f);
		else if (idx == DrumParam.Param3)
			return (int) (rattle * 100f);
		return 0;
	}

	public void setNoise(int val) {
		noiseGen.setColor((Colour) Constants.ratio(val, Colour.values()));
	}

	public void setMix(float mix) {
		this.mix = Math.max(0f, Math.min(1f, mix));
		dirty = true;
	}

	public void setRattle(float rattle) {
		this.rattle = Math.max(0f, Math.min(1f, rattle));
		dirty = true;
	}

	private void update() {
		if (!dirty)
			return;
		atkFrames = Math.max(1, (attack * SR) / 1000);
		dkFrames = Math.max(0, (decay * SR) / 1000);
		decayStart = atkFrames;

		noiseFrames = Math.max(1, (int) (atkFrames + dkFrames * (0.25f + 0.75f * (1f - noise))));
		invNoiseFrames = 1.0f / noiseFrames;

		final float cutHz = 3000f + brightness * 8000f;
		noiseAlpha = (float) Math.exp(-2.0 * Math.PI * cutHz / SR);

		// Offload envelope frames to the Rattle instance for next()
		int  rattleFrames = Math.max(1, (int) (dkFrames * (0.45f + brightness * 0.35f)));
 		snares.setEnvelopeFrames(rattleFrames);

		dirty = false;
	}

	@Override
	protected void generate() {
		update();

		final float startFreq = pitch.getFrequency();
		final float endFreq = startFreq * 0.98f;
		final int dk = dkFrames;
		final float linearInc = dk > 0 ? (endFreq - startFreq) / dk : 0f;
		final float localRattle = rattle;
		final float localMix = mix;
		final int dStart = decayStart;

		for (int i = 0; i < N_FRAMES; i++) {
			float noiseEnv = Math.max(0f, 1.0f - i * invNoiseFrames);
			int n = i - dStart;
			n = Math.max(0, Math.min(n, dk));
			float currentFreq = startFreq + n * linearInc;

			// Use Phase for blended phase output and advance during blending
			float phase = body.next(currentFreq * ISR);

			float sine = lookup.forPhase(phase);
			float rattleSum = 0f;
			if (localRattle > 0f) {
				// start when decay starts (index = frameCounter + i)
				int rIdx = frameCounter + i - dStart;
				if (rIdx == 0)
					snares.startEnvelope(localRattle);
				if (rIdx >= 0)
					rattleSum = snares.next(currentFreq); // stateful, advances envelope + phases
			}

			// shape noise via NoiseGen one-pole (consumes generator's next())
			noiseFiltState = noiseGen.onePole(noiseFiltState, noiseAlpha);
			final float shapedNoise = noiseFiltState * noiseEnv;

			// Apply tonal/atonal multipliers: tonal affects the pitched sine only,
			// atonal affects noise and rattle components (non-pitched elements).
			final float pitchedPart = sine * (1.0f - localMix) * tonal;
			final float nonPitched = (shapedNoise * localMix) + (rattleSum * RATTLE_RATIO);
			final float out = pitchedPart + nonPitched * atonal;
			work[i] = out * 0.9f;
		}
	}

	@Override
	public void trigger(int data2) {
		super.trigger(data2);
		// Blend window in samples (≈5 ms at SR); clamp to at least 1 to avoid a zero-length blend.
		int blendSamples = Math.max(1, Math.round(0.005f * SR));
		body.trigger(blendSamples);
		snares.resetPhase();
	}

	public void resetPhase() { // not used, handled by trigger
	    body.reset();
	    snares.resetPhase();
	}

}
