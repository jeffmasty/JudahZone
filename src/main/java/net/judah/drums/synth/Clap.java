package net.judah.drums.synth;

import java.util.function.Consumer;
import java.util.function.Supplier;

import judahzone.data.Env;
import judahzone.data.Filter;
import judahzone.data.Stage;
import judahzone.fx.op.NoiseGen.Colour;
import net.judah.drums.DrumType;
import net.judah.drums.synth.Clap.ClapParams;
import net.judah.drums.synth.DrumParams.DrumParam;
import net.judah.drums.synth.DrumParams.Freqs;
import net.judah.drums.synth.DrumParams.Settings;
import net.judah.midi.Actives;

/** Multi-layered hand clap: several staggered noise bursts, colored and mildly reverberant. */
public class Clap extends DrumOsc implements Consumer<ClapParams>, Supplier<ClapParams> {
	public static record ClapParams(int shimmer, float colour, int room) { }

	public static Settings SETTINGS = new Settings(
		new Stage(0.50f, 50), new Env(18, 23, 10),
		new Freqs(	new Filter(400, 0f),
				new Filter(2600, 9f),
				new Filter(6666, 9f)),
		Colour.WHITE,
		new String[] { "Shimmer", "Colour", "Room" });

	private static final float R_FB = 0.25f;
	private static final float R_WRITE = R_FB + 0.25f;
	private static final int LAYERS = 4; // number of sub-noise bursts (1..5)

	/* user / preset parameters (mapped from ClapParams) */

	private float colour = 0.5f; // 0 = dark (lowpassed), 1 = bright (raw noise)
	private int shimmer = 30;

	// TODO private final Phase[] layerPhases  = new Phase[MAX_LAYERS];

	private float noiseFiltState = 0f; // one-pole state for noise colouring
	/* per-layer preallocated arrays (max 5 layers) */
	private static final int MAX_LAYERS = 5;
	private final int[] layerDelay = new int[MAX_LAYERS];
	private final int[] layerDecay = new int[MAX_LAYERS];
	private final float[] layerInvDecay = new float[MAX_LAYERS];
	private final float[] layerAmp = new float[MAX_LAYERS];

	/* tiny circular buffer for a cheap room/comb tail (kept as field) */
	private final float[] roomBuf = new float[128];
	private int roomIdx = 0;

	private int transientFrames;
	private float invTransient;
	private float noiseAlpha;
	private int cachedLayers;

	public Clap(Actives actives) {
		super(DrumType.Clap, SETTINGS, actives);
		network(shimmer * 0.01f, 419, 590);
	}

	@Override public void accept(ClapParams t) {
		setShimmer(t.shimmer());
		setColour(t.colour());
		setRoom(t.room());
	}

	@Override public ClapParams get() {
		return new ClapParams(shimmer, colour, room);
	}

	@Override public void set(DrumParam idx, int knob) {
		if (idx == DrumParam.Param1)
			setShimmer(knob);
		else if (idx == DrumParam.Param2)
			setColour(knob * 0.01f);
		else if (idx == DrumParam.Param3)
			setRoom(knob);
	}

	@Override public int get(DrumParam idx) {
		if (idx == DrumParam.Param1)
			return shimmer;
		else if (idx == DrumParam.Param2)
			return (int) (colour * 100f);
		else if (idx == DrumParam.Param3)
			return room;
		return 0;
	}

	public void setShimmer(int fb) {
		shimmer = Math.max(0, Math.min(100, fb));
		allpass(shimmer * 0.01f);
	}

	public void setColour(float colour) {
		this.colour = Math.max(0f, Math.min(1f, colour));
		dirty = true;
	}

	private void update() {
		if (!dirty)
			return;
		/* transientFrames originally computed from attack value.
		 * Keep same approach but ensure at least 1 frame. */
		transientFrames = Math.max(1, (attack * SR) / 1000);
		invTransient = 1.0f / transientFrames;
		int dkFrames = Math.max(0, (decay * SR) / 1000);

		// Base cutoff mapped from colour param; include `hz` influence so pitch
		// changes alter perceived timbre (user-requested responsiveness).
		float baseCut = 3000f + colour * 12000f;
		float hzInfluence = (pitch.getFrequency() - 450f) * 0.2f; // modest influence, avoids extremes
		float cutHz = Math.max(100f, baseCut + hzInfluence);
		noiseAlpha = (float) Math.exp(-2.0 * Math.PI * cutHz / SR);

		cachedLayers = Math.max(1, Math.min(MAX_LAYERS, LAYERS));
		for (int l = 0; l < cachedLayers; l++) {
			int delay = (int) (l * (transientFrames * 0.5f)) + l * 2;
			layerDelay[l] = delay;

			int decayValue = Math.max(1, (int) (dkFrames * (0.6f + room * 0.8f) * (1.0f - l * 0.12f)));
			layerDecay[l] = decayValue;
			layerInvDecay[l] = 1.0f / decayValue;

			layerAmp[l] = 1.0f - l * 0.14f;
		}
		for (int l = cachedLayers; l < MAX_LAYERS; l++) {
			layerDelay[l] = Integer.MAX_VALUE;
			layerDecay[l] = 1;
			layerInvDecay[l] = 1.0f;
			layerAmp[l] = 0f;
		}
		dirty = false;
	}

	@Override protected void generate() {
		update();

		final int transFrames = transientFrames;
		final float invTrans = invTransient;
		final float noiseCutAlpha = noiseAlpha;
		final int nLayers = cachedLayers;

		/* Blend factor: 0.0 = fully crisp (attack <= 10), 1.0 = fully smooth (attack >= 20).
		 * Linear interpolation between 10..20 as requested. */
		final float blend = Math.max(0f, Math.min(1f, (attack - 10f) / 10f));
		/* Crisp envelope window in frames (very short). 3ms chosen to sound sharp. */
		final int crispFrames = Math.max(1, Math.round(0.003f * SR));

		for (int i = 0; i < N_FRAMES; i++) {
			// Use the shared/thread-local NoiseGen for raw noise source. This centralizes
			// RNG and colored-noise code and avoids per-class xorshift duplication.
			float rawNoise = noiseGen.next();

			// per-instance one-pole to shape brightness according to `colour`.
			noiseFiltState = noiseGen.onePole(noiseFiltState, noiseCutAlpha, rawNoise);
			float shapedNoise = rawNoise * colour + noiseFiltState * (1.0f - colour);

			float sum = 0f;
			for (int l = 0; l < nLayers; l++) {
				int idx = i - layerDelay[l];
				if (idx >= 0 && idx < layerDecay[l]) {
					float env = 1.0f - idx * layerInvDecay[l];
					sum += shapedNoise * env * layerAmp[l];
				}
			}

			/* Compute two transient shapes and mix them:
			 *  - transSmooth: original behavior (linear ramp across transientFrames)
			 *  - transCrisp: short, sharp linear decay across crispFrames
			 * Final trans = blend * transSmooth + (1-blend) * transCrisp
			 */
			float transSmooth = (i < transFrames) ? 1.0f - i * invTrans : 0f;
			float transCrisp = (i < crispFrames) ? 1.0f - (i / (float) crispFrames) : 0f;
			float trans = blend * transSmooth + (1.0f - blend) * transCrisp;

			float transWindow = i < transFrames || i < crispFrames ? trans : 0f;
			sum *= transWindow;

			float roomIn = roomBuf[roomIdx];
			sum += roomIn * R_FB;
			roomBuf[roomIdx] = sum * R_WRITE;
			if (++roomIdx >= roomBuf.length)
				roomIdx = 0;

			work[i] = sum * 0.9f;
		}
	}

}
