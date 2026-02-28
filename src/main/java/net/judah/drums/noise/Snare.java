package net.judah.drums.noise;

import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.sound.midi.ShortMessage;

import judahzone.api.Hz;
import judahzone.api.RTNotes;
import judahzone.data.Postage;
import judahzone.filter.Coord;
import judahzone.filter.FilterT;
import judahzone.fx.Gain.GainT;
import judahzone.fx.op.Noise.Colour;
import judahzone.fx.op.Rattle;
import judahzone.util.Phase;
import net.judah.drums.Drama.Freqs;
import net.judah.drums.DrumSetup;
import net.judah.drums.DrumType;
import net.judah.drums.noise.Snare.SnareParams;

/** Snare: noise + pitched tone + sympathetic rattle. */
public class Snare extends NoiseOsc implements Consumer<SnareParams>, Supplier<SnareParams> {
	public static record SnareParams(Colour colour, float mix, float rattle) { }
	public static final float DEFAULT_HZ = 391f;
	public static DrumSetup SETTINGS = new DrumSetup( DrumType.Snare,
			new GainT(0.55f), new Postage(8, 25),
			new Freqs(	new Coord(180, 6f),
						new FilterT(DEFAULT_HZ, 0f, 0.5f),
						new Coord(10000, 6f)),
			new Hz(DEFAULT_HZ),
			new String[] { "Color", "Mix", "Rattle" });

	private static final float RATTLE_RATIO = 2.4f; // std multiplier to the Rattle output
	private final float noise = 0.5f; // de-parameterized noise level (0..1, where 1 is longest)
	private final float brightness = 0.6f; // de-parameterized brightness for rattle length (0..1, where 1 is longest)

	private static final float[] RATTLE_RATIOS = new float[] { 3.0f, 3.7f, 4.3f };
	private final Rattle snares;
	private float rattle = 0.2f;
	private float mix = 0.45f;
	private float noiseFiltState = 0f;

	private final Phase body = new Phase(PHASE_BLEND);

	private int atkFrames;
	private int dkFrames;
	private int decayStart;
	private int noiseFrames;
	private float invNoiseFrames;
	private float noiseAlpha;
	private int frameCounter;

	public Snare(RTNotes actives) {
		super(DrumType.Snare, SETTINGS, Colour.WHITE, actives);
		tonal = 0.5f;
		atonal = 2f;
		env.addPhase(body);
		snares = new Rattle(RATTLE_RATIOS, SR, env);
		params();
	}

	@Override public void accept(SnareParams t) {
		noiseGen.setColour(t.colour());
		setMix(t.mix());
		setRattle(t.rattle());
	}

	@Override public SnareParams get() {
		return new SnareParams(noiseGen.getColour(), mix, rattle);
	}

	@Override public void set(int idx, int knob) {
		float val = knob * 0.01f;
		if (idx == 0)
			setNoise(knob);
		else if (idx == 1)
			setMix(val);
		else if (idx == 2)
			setRattle(val);
	}

	@Override public int get(int idx) {
		if (idx == 0)
			return (noiseGen.getColourKnob());
		else if (idx == 1)
			return (int) (mix * 100f);
		else if (idx == 2)
			return (int) (rattle * 100f);
		return 0;
	}

	@Override
	public String val(int idx) {
		if (idx == 0) return noiseGen.getColour().name();
		return super.val(idx);
	}

	public void setNoise(int val) {
		noiseGen.setColourKnob(val);
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
		atkFrames = Math.max(1, env.getAttackSamples());
		dkFrames = Math.max(0, env.getDecaySamples());
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

	@Override public void trigger(ShortMessage midi) {
		super.trigger(midi);
		frameCounter = 0;
	}

	@Override
	protected void generate() {
		update();

		final float startFreq = getFundamental();
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

			float sine = Phase.sin(phase);
			float rattleSum = 0f;
			if (localRattle > 0f) {
				// start when decay starts (index = frameCounter + i)
				int rIdx = frameCounter + i - dStart;
				if (rIdx == 0)
					snares.startEnvelope(localRattle);
				if (rIdx >= 0)
					rattleSum = snares.next(currentFreq); // stateful, advances envelope + phases
			}

			frameCounter += N_FRAMES;
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


}
