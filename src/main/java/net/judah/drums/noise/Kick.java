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
import judahzone.util.Phase;
import net.judah.drums.Drama.Freqs;
import net.judah.drums.DrumSetup;
import net.judah.drums.DrumType;
import net.judah.drums.noise.Kick.KickParams;

/** 808-style kick: exponential pitch sweep with beater click transient. */
public class Kick extends NoiseOsc implements Consumer<KickParams>, Supplier<KickParams> {
	public static record KickParams(float strike, short bend, float tone) { }
	private static final float DEFAULT_HZ = 55f;
	public static DrumSetup SETTINGS = new DrumSetup ( DrumType.Kick,
			new GainT(0.7f), new Postage(5, 30),
			new Freqs(
					new Coord(45, 9f),
					new FilterT(new Coord(DEFAULT_HZ, 0f)),
					new Coord(180, 6f)),
			new Hz(DEFAULT_HZ),
			new String[] { "Strike", "Bend", "Tone" });

	private float strike = 0.65f;  // Beater click amount (0–1, attack transient intensity)
	private short bend = -6;        // Pitch bend in semitones (±11)

	private final Phase bodyPhase      = new Phase(PHASE_BLEND); // main pitch / body oscillator
	private final Phase clickPhase     = new Phase(PHASE_BLEND); // short transient/click
	//private final Phase subOscPhase    = new Phase(PHASE_BLEND); // low sub / thump oscillator // TODO

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
	private int attack; // attack samples hoisted at trigger time
	private int decay; // decay samples hoisted at trigger time

	public Kick(RTNotes actives) {
		super(DrumType.Kick, SETTINGS, Colour.BROWN, actives);
		env.addPhase(bodyPhase);
		env.addPhase(clickPhase);
		// env.addPhase(subOscPhase); TODO
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

	@Override public void set(int idx, int knob) {
		float val = knob * 0.01f;
		if (idx == 0)
			setStrike(val);
		else if (idx == 1) {
			float bend = knob * 0.01f * 22f - 11f;
			setBend((short)bend);
		} else if (idx == 2)
			setTone(val);
	}

	@Override public int get(int idx) {
		if (idx == 0)
			return (int) (strike * 100f);
		else if (idx == 1)
			return (int) (((bend + 11f) / 22f) * 100f);
		else if (idx == 2)
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

	@Override public void trigger(ShortMessage midi) {
		update();
		super.trigger(midi);
		currentFreq = startFreq;
		sweepFrameCounter = 0;

		attack = env.getAttackSamples();
		decay = env.getDecaySamples();

	}

	private void update() {
		if (!dirty)
			return;

		clickFrames = Math.max(1, (int)(env.getAttackSamples() * 0.3f));
		invClickFrames = 1.0f / clickFrames;

		final int dkFrames = env.getDecaySamples();
		expMode = dkFrames > 0; // && curve > 0.5f;

		final float clickCutHz = Math.max(200f, 800f + strike * 3000f + (fundamental - 50f) * 0.05f);
		clickNoiseAlpha = (float)Math.exp(-2.0 * Math.PI * clickCutHz / SR);

		// Compute bend freq sweep using Bongo's algorithm
		startFreq = Math.max(20f, fundamental);
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

		final int decayStart = attack;
		final int decayEnd = decayStart + decay;

		for (int i = 0; i < N_FRAMES; i++) {
			final float clickAmount = i < clickFrames ? 1.0f - i * invClickFrames : 0f;

			if (sweepFrameCounter >= decayStart && sweepFrameCounter < decayEnd) {
				currentFreq = expMode ? currentFreq * freqMulPerFrame : currentFreq;
			} else if (sweepFrameCounter >= decayEnd) {
				currentFreq = endFreq;
			}

			// Use Phase objects for retrigger-safe blending and to advance phases
			float sine = Phase.sin(bodyPhase.next(currentFreq * ISR));
			float click = Phase.sin(clickPhase.next(clickPhaseInc)) * clickAmount;

			float rawNoise = noiseGen.next();
			clickNoiseState = clickNoiseState * clickNoiseAlpha + rawNoise * (1.0f - clickNoiseAlpha);
			float shapedClickNoise = (rawNoise * 0.25f + clickNoiseState * 0.75f) * clickAmount;

			// Mix: scale the pitched sine with `tone`, and scale everything else with `nonTone`.
			float nonSine = (click * strike) + (shapedClickNoise * strike * 0.9f);
			work[i] = sine * (1.0f - strike * 0.5f) * tonal + nonSine * atonal;

			sweepFrameCounter++;
		}
	}

}
