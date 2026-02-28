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
import judahzone.fx.op.Wobble;
import judahzone.util.Constants;
import judahzone.util.Phase;
import net.judah.drums.Drama.Freqs;
import net.judah.drums.Drum.OverSampled;
import net.judah.drums.DrumSetup;
import net.judah.drums.DrumType;
import net.judah.drums.noise.OHat.OHatParams;

/** Open Hi‑Hat: band‑shaped noise, density/brightness/wobble and a choke envelope. */
public class OHat extends NoiseOsc implements OverSampled, Consumer<OHatParams>, Supplier<OHatParams> {
	public static record OHatParams(int shimmer, float wobble, float density) {}
	public static final float DEFAULT_HZ = 4500f;
	public static DrumSetup SETTINGS = new DrumSetup( DrumType.OHat,
			new GainT(2.5f), new Postage(15, 170),
			new Freqs(	new Coord(2100, 3f),
					new FilterT(DEFAULT_HZ, 9f, 0.5f),
					new Coord(16000, 6f)),
			new Hz(DEFAULT_HZ),
			new String[] { "Shimmer", "Wobble", "Density"});

	private static final float hiCutBase = 18000;
	private static final float midCutBase = 10000;

	private float wobble = 0.2f;
	private float density = 0.5f;
	private int shimmer = 30;

	private final Phase wobbleLFO = new Phase(); // wobble LFO with retrigger blending
	private final Wobble transients = new Wobble(ISR_OVER);

	private float filtHiState = 0f;
	private float filtMidState = 0f;

	private volatile int chokeFramesLeft = 0;
	private volatile int chokeTotalFrames = 0;

	public OHat(RTNotes actives) {
		super(DrumType.OHat, SETTINGS, Colour.VELVET, actives);
		network(shimmer * 0.01f, 373, 599);

		env.addPhase(wobbleLFO);
		env.addPhase(transients);
	}

	@Override public void accept(OHatParams t) {
		setWobble(t.wobble());
		setDensity(t.density());
		setShimmer(t.shimmer());
	}

	@Override public OHatParams get() {
		return new OHatParams(shimmer, wobble, density);
	}

	@Override public void set(int idx, int knob) {
		if (idx == 0)
			setShimmer(knob);
		else if (idx == 1)
			setWobble(knob * 0.01f);
		else if (idx == 2)
			setDensity(Constants.inverseLog(knob));
	}

	@Override public int get(int idx) {
		if (idx == 0)
			return shimmer;
		else if (idx == 1)
			return (int) (wobble * 100f);
		else if (idx == 2)
			return Constants.reverseInverseLog(density);
		return 0;
	}

	public void setWobble(float w) {
		wobble = w;
		dirty = true;
	}

	public void setDensity(float d) {
		density = d;
	}

	public void setShimmer(int shimmer) {
		this.shimmer = shimmer;
		allpass(shimmer * 0.01f);
	}

	// pass factored attack frames into Wobble.update
	private void update() {
	    if (!dirty)
	        return;
	    // attackFrames must be factored for oversampled helpers (Wobble expects factored frames)
	    int transientFrames = Math.max(1, env.getAttackSamples() * FACTOR);
	    final float bodyRef = SETTINGS.freqs().body().hz();
	    final float hzInfluence = (bodyRef - 4500f) * 0.2f;
	    final float hiCutHz = Math.max(200f, hiCutBase + hzInfluence);
	    final float midCutHz = Math.max(100f, midCutBase + hzInfluence);
	    final float wobbleRateHz = 6.0f * (0.2f + wobble * 1.8f);
	    transients.update(transientFrames, hiCutHz, midCutHz, wobbleRateHz);
	    dirty = false;
	}

	@Override
	public void trigger(ShortMessage midi) {
		super.trigger(midi);
		dirty = true; // for transients
	}

	@Override
	protected void generate() {
		update();

		final int factor = FACTOR;
		int localChokeLeft = chokeFramesLeft;
		int localChokeTotal = chokeTotalFrames;

		int transientFrames = transients.transientFrames;
		float invTransient = transients.invTransient;
		float hiAlpha = transients.hiAlpha;
		float midAlpha = transients.midAlpha;
		float wobbleInc = transients.wobbleInc;

		int idx = 0;
		for (int i = 0; i < N_FRAMES; i++) {
			final float trans = i < transientFrames ? 1.0f - i * invTransient : 0f;

			float chokeMul = 1.0f;
			if (localChokeLeft > 0 && localChokeTotal > 0) {
				chokeMul = (float)localChokeLeft / (float)localChokeTotal;
				localChokeLeft--;
			}

			for (int s = 0; s < factor; s++) {
				float raw = noiseGen.next();

				filtHiState = filtHiState * hiAlpha + raw * (1.0f - hiAlpha);
				filtMidState = filtMidState * midAlpha + raw * (1.0f - midAlpha);

				float wob = 1.0f - wobble * 0.35f + 0.35f * Phase.sin(wobbleLFO.next(wobbleInc));

				float gate = Math.abs(raw) < (1.0f - density) ? 0f : 1f;

				float shaped = filtMidState * 0.6f + filtHiState * 0.9f;

				work[idx++] = shaped * trans * chokeMul * wob * gate * density * 0.8f;
			}
		}

		chokeFramesLeft = localChokeLeft;
		if (localChokeLeft <= 0)
			chokeTotalFrames = 0;
	}

//	public void resetPhase() {
//		transients.reset();
//	}

	protected void choke() {
		int frames = Math.max(1, (int)(0.03f * SR));
		if (frames > N_FRAMES)
			frames = N_FRAMES;
		chokeTotalFrames = frames;
		chokeFramesLeft = frames;
	}
}
