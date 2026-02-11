package net.judah.drums.synth;

import java.util.function.Consumer;
import java.util.function.Supplier;

import judahzone.data.Env;
import judahzone.data.Filter;
import judahzone.data.Stage;
import judahzone.fx.op.NoiseGen.Colour;
import net.judah.drums.DrumType;
import net.judah.drums.synth.Burst.BurstParams;
import net.judah.drums.synth.DrumParams.DrumParam;
import net.judah.drums.synth.DrumParams.Freqs;
import net.judah.drums.synth.DrumParams.Settings;
import net.judah.midi.Actives;

/** Burst kick: filtered noise with envelope-modulated resonant lowpass sweep.
 * 	-  	Noise coloration (tone param): Controls the one-pole filter cutoff for the raw noise source. Higher tone = brighter, less filtering.
 * 	-  	Boom character (boom param): Shapes the decay envelope and filter sweep dynamics.
 * 			Higher boom creates slower decay and more pronounced resonant character.
 * 	-	Filter sweep: Cutoff starts high and drifts downward over the decay phase, controlled by the envelope and boom value. (TODO?)
 * 	-	Envelope shaping: Fast attack with decay shaped by the boom parameter—higher boom yields a slower, more analog-sounding tail.
 * 	-	All-pass network: Connected via network() for optional reverb character, scaled by shimmer.
 * 	-	CPU-efficient: Single per-sample one-pole filters, no complex resonant modeling—suitable for stacking multiple voices.
 * 	-	Tweak filterStartHz, filterEndHz, and the decay shape curve to dial in your desired kick character (house, techno, industrial, etc.).
 * */
public class Burst extends DrumOsc implements Consumer<BurstParams>, Supplier<BurstParams> {
	public static record BurstParams(float tone, float boom, int room) { }

	public static Settings SETTINGS = new Settings(
		new Stage(0.8f, 50),
		new Env(8, 110, 15),
		new Freqs(
			new Filter(40, 6f),
			new Filter(120, 0f),
			new Filter(400, 3f)),
		Colour.WHITE,
		new String[] { "Tone", "Boom", "Room" });

	private float tone = 0.5f;      // filter sweep range (0=minimal, 1=wide)
	private float boom = 0.6f;      // envelope decay character (0=punchy, 1=boom)
	private int shimmer = 30;       // allpass feedback

	private float noiseFiltState = 0f;
	private float envFiltState = 0f;
	private int atkFrames;
	private int dkFrames;
	private float invDecay;
	private float noiseAlpha;
	private float envAlpha;
	// private float filterStartHz;
	// private float filterEndHz;

	public Burst(Actives actives) {
		super(DrumType.Kick, SETTINGS, actives);
		network(shimmer * 0.01f, 419, 590);
	}

	@Override public void accept(BurstParams t) {
		setTone(t.tone());
		setBoom(t.boom());
		setRoom(t.room());
	}

	@Override public BurstParams get() {
		return new BurstParams(tone, boom, room);
	}

	@Override public void set(DrumParam idx, int knob) {
		float val = knob * 0.01f;
		if (idx == DrumParam.Param1)
			setTone(val);
		else if (idx == DrumParam.Param2)
			setBoom(val);
		else if (idx == DrumParam.Param3)
			setRoom(knob);
	}

	@Override public int get(DrumParam idx) {
		if (idx == DrumParam.Param1)
			return (int) (tone * 100f);
		else if (idx == DrumParam.Param2)
			return (int) (boom * 100f);
		else if (idx == DrumParam.Param3)
			return room;
		return 0;
	}

	public void setTone(float t) {
		tone = Math.max(0f, Math.min(1f, t));
		dirty = true;
	}

	public void setBoom(float b) {
		boom = Math.max(0f, Math.min(1f, b));
		dirty = true;
	}

	private void update() {
		if (!dirty)
			return;

		atkFrames = Math.max(1, (attack * SR) / 1000);
		dkFrames = Math.max(1, (decay * SR) / 1000);
		invDecay = 1.0f / dkFrames;

		// Noise coloration: brighter tone -> less filtering
		final float cutHz = 4000f + tone * 8000f;
		noiseAlpha = (float) Math.exp(-2.0 * Math.PI * cutHz / SR);

		// Envelope filter sweep: boom parameter controls decay rate shape
		// Higher boom = slower decay = longer sweep = more "analog" character
		final float envCutHz = 200f + boom * 1800f;
		envAlpha = (float) Math.exp(-2.0 * Math.PI * envCutHz / SR);

		// TODO Filter sweep: start high, sweep down. Tone controls range.
		// filterStartHz = 8000f + tone * 8000f;  // 8k .. 16k
		// filterEndHz = 200f + tone * 1500f;     // 200 .. 1700

		dirty = false;
	}

	@Override protected void generate() {
		update();

		final int atk = atkFrames;
		final int dk = dkFrames;
		final float invDk = invDecay;
		final float localBoom = boom;
		// final float startHz = filterStartHz;
		// final float endHz = filterEndHz;
		// final float freqSweep = dk > 0 ? (endHz - startHz) / dk : 0f;

		for (int i = 0; i < N_FRAMES; i++) {
			// Attack ramp then decay (envelope controls amplitude and filter sweep)
			float env;
			if (i < atk) {
				env = i / (float) atk;
			} else {
				int decayIdx = i - atk;
				if (decayIdx < dk) {
					// Decay shaped by boom: higher boom = slower decay
					float decayProgress = decayIdx * invDk;
					float decayShape = 1.0f - decayProgress * (1.0f - localBoom * 0.5f);
					env = Math.max(0f, decayShape);
				} else {
					env = 0f;
				}
			}

			// Raw noise, one-pole colored by tone parameter
			float raw = noiseGen.next();
			noiseFiltState = noiseFiltState * noiseAlpha + raw * (1.0f - noiseAlpha);

			// Envelope filter: smooth modulation of the sweep direction
			envFiltState = envFiltState * envAlpha + env * (1.0f - envAlpha);

			// TODO Filter sweep: modulated by envelope, sweeps from high to low
			// int sweepIdx = Math.max(0, Math.min(i - atk, dk - 1));
			// float currentCutHz = startHz + sweepIdx * freqSweep;

			// Simplified resonant lowpass approximation: scale noise by envelope
			// and add a small boost for resonance character
			float filterBias = 1.0f + boom * 0.3f;  // resonance emphasis
			float shaped = noiseFiltState * env * filterBias;

			// Scale output with envelope and boom multiplier
			work[i] = shaped * envFiltState * 0.9f;
		}
	}
}
