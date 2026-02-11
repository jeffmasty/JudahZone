package net.judah.drums.synth;

import java.util.function.Consumer;
import java.util.function.Supplier;

import judahzone.data.Env;
import judahzone.data.Filter;
import judahzone.data.Frequency;
import judahzone.data.Stage;
import judahzone.fx.op.NoiseGen.Colour;
import judahzone.util.Phase;
import net.judah.drums.DrumType;
import net.judah.drums.synth.DrumParams.DrumParam;
import net.judah.drums.synth.DrumParams.Freqs;
import net.judah.drums.synth.DrumParams.Settings;
import net.judah.drums.synth.Kick.KickParams;
import net.judah.midi.Actives;

/** 808-style kick: exponential pitch sweep with beater click transient. */
public class Kick extends DrumOsc implements Consumer<KickParams>, Supplier<KickParams> {
	public static record KickParams(float strike, short bend, float tone) { }

	public static Settings SETTINGS = new Settings (
			new Stage(0.7f, 50),
			new Env(5, 30),
			new Freqs(
					new Filter(45, 9f),
					new Filter(55, 0f),
					new Filter(180, 6f)),
			Colour.BROWN,
			new String[] { "Strike", "Bend", "Tone" });

	private float strike = 0.65f;  // Beater click amount (0–1, attack transient intensity)
	private short bend = -6;        // Pitch bend in semitones (±11)

	private final Phase bodyPhase      = new Phase(); // main pitch / body oscillator
	private final Phase clickPhase     = new Phase(); // short transient/click
	private final Phase subOscPhase    = new Phase(); // low sub / thump oscillator

	// Cached / dirty pattern
	private int clickFrames;
	private float invClickFrames;
	private boolean expMode;

	// Pitch sweep state (persists across buffers)
	private float startFreq;
	private float endFreq;
	private float currentFreq;
	private float freqMulPerFrame;
	private int sweepFrameCounter = 0;

	private final float clickFreq = 150f;
	private final float clickPhaseInc = clickFreq * ISR;
	private float clickNoiseState = 0f;
	private float clickNoiseAlpha = 0.9f; // computed in update()

	public Kick(Actives actives) {
		super(DrumType.Kick, SETTINGS, actives);
	}

	@Override
	public void accept(KickParams t) {
		strike = t.strike();
		bend = t.bend();
		tonal = t.tone();
		dirty = true;
	}

	@Override public KickParams get() {
		return new KickParams(strike, bend, tonal);
	}

	@Override public void set(DrumParam idx, int knob) {
		float val = knob * 0.01f;
		if (idx == DrumParam.Param1)
			setStrike(val);
		else if (idx == DrumParam.Param2) {
			float bend = knob * 0.01f * 22f - 11f;
			setBend((short)bend);
		} else if (idx == DrumParam.Param3)
			setTone(val);
	}

	@Override public int get(DrumParam idx) {
		if (idx == DrumParam.Param1)
			return (int) (strike * 100f);
		else if (idx == DrumParam.Param2)
			return (int) (((bend + 11f) / 22f) * 100f);
		else if (idx == DrumParam.Param3)
			return (int) (tonal * 100f);
		return 0;
	}

	public void setStrike(float strike) {
		this.strike = Math.max(0f, Math.min(1f, strike));
		dirty = true;
	}

	public void setBend(short bend) {
		this.bend = (short) Math.max(-11, Math.min(11, bend));
		dirty = true;
	}

	/** Set the relative amplitude of the pitched sine tone. 0 = silent, 1 = default. */
	public void setTone(float tone) {
		tonal = Math.max(0f, Math.min(1f, tone)); // clamp reasonable range
	}

	/** Get the current tone multiplier. */
	public float getTone() { return tonal; }

	@Override public void trigger(int data2) {
		update();
		super.trigger(data2);
		currentFreq = startFreq;
		sweepFrameCounter = 0;
		// enable a short blend (avoid click) for all phases ~5ms
		int blendSamples = Math.max(1, Math.round(0.005f * SR));
		bodyPhase.trigger(blendSamples);
		clickPhase.trigger(blendSamples);
		subOscPhase.trigger(blendSamples);
	}

	private void update() {
		if (!dirty)
			return;

		clickFrames = Math.max(1, (int)(atkTarget * 0.3f));
		invClickFrames = 1.0f / clickFrames;

		final int dkFrames = dkTarget;
		expMode = dkFrames > 0; // && curve > 0.5f;

		final float clickCutHz = Math.max(200f, 800f + strike * 3000f + (pitch.getFrequency() - 50f) * 0.05f);
		clickNoiseAlpha = (float)Math.exp(-2.0 * Math.PI * clickCutHz / SR);

		// Compute bend freq sweep using Bongo's algorithm
		startFreq = Math.max(20f, pitch.getFrequency());
		int midi = Frequency.hzToMidi(startFreq) + bend;
		endFreq = Frequency.midiToHz(midi);

		if (dkFrames > 0) {
			float ratio = endFreq / startFreq;
			freqMulPerFrame = (float) Math.pow(ratio, 1.0f / dkFrames);
		} else {
			freqMulPerFrame = 1.0f;
		}

		dirty = false;
	}

	@Override protected void generate() {
		update();

		final int atkFrames = atkTarget;
		final int dkFrames = dkTarget;
		final int decayStart = atkFrames;
		final int decayEnd = decayStart + dkFrames;

		for (int i = 0; i < N_FRAMES; i++) {
			final float clickAmount = i < clickFrames ? 1.0f - i * invClickFrames : 0f;

			if (sweepFrameCounter >= decayStart && sweepFrameCounter < decayEnd) {
				currentFreq = expMode ? currentFreq * freqMulPerFrame : currentFreq;
			} else if (sweepFrameCounter >= decayEnd) {
				currentFreq = endFreq;
			}

			// Use Phase objects for retrigger-safe blending and to advance phases
			float sine = lookup.forPhase(bodyPhase.next(currentFreq * ISR));
			float click = lookup.forPhase(clickPhase.next(clickPhaseInc)) * clickAmount;

			float rawNoise = noiseGen.next();
			clickNoiseState = clickNoiseState * clickNoiseAlpha + rawNoise * (1.0f - clickNoiseAlpha);
			float shapedClickNoise = (rawNoise * 0.25f + clickNoiseState * 0.75f) * clickAmount;

			// Mix: scale the pitched sine with `tone`, and scale everything else with `nonTone`.
			float nonSine = (click * strike) + (shapedClickNoise * strike * 0.9f);
			work[i] = sine * (1.0f - strike * 0.5f) * tonal + nonSine * atonal;

			sweepFrameCounter++;
		}
	}

	/** Reset all phases to zero (hard reset). */
	public void resetPhase() {
		bodyPhase.reset();
		clickPhase.reset();
		subOscPhase.reset();
	}
}
