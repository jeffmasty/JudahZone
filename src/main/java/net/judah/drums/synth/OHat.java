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
import net.judah.drums.synth.DrumOsc.OverSampled;
import net.judah.drums.synth.DrumParams.DrumParam;
import net.judah.drums.synth.DrumParams.Freqs;
import net.judah.drums.synth.DrumParams.Settings;
import net.judah.drums.synth.OHat.OHatParams;
import net.judah.midi.Actives;

/** Open Hi‑Hat: band‑shaped noise, density/brightness/wobble and a choke envelope. */
public class OHat extends DrumOsc implements OverSampled, Consumer<OHatParams>, Supplier<OHatParams> {
	public static record OHatParams(int shimmer, float wobble, float density) {}

	public static Settings SETTINGS = new Settings(
			new Stage(3.5f, 50), new Env(8, 85),
		new Freqs(	new Filter(2100, 3f),
					new Filter(4500, 9f),
					new Filter(16000, 6f)),
		Colour.VELVET,
		new String[] { "Shimmer", "Wobble", "Density"});

	private static final float hiCutBase = 18000;
	private static final float midCutBase = 10000;

	private float wobble = 0.2f;
	private float density = 0.5f;
	private int shimmer = 30;

	private final Phase wobbleLFO = new Phase(); // wobble LFO with retrigger blending

	private float filtHiState = 0f;
	private float filtMidState = 0f;

	private volatile int chokeFramesLeft = 0;
	private volatile int chokeTotalFrames = 0;
	private int transientFrames;
	private float invTransient;
	private float hiAlphaCached;
	private float midAlphaCached;
	private float wobbleIncCached;

	public OHat(Actives actives) {
		super(DrumType.OHat, SETTINGS, actives);
		network(shimmer * 0.01f, 373, 599);
	}

	@Override public void accept(OHatParams t) {
		setWobble(t.wobble());
		setDensity(t.density());
		setShimmer(t.shimmer());
	}

	@Override public OHatParams get() {
		return new OHatParams(shimmer, wobble, density);
	}

	@Override public void set(DrumParam idx, int knob) {
		if (idx == DrumParam.Param1)
			setShimmer(knob);
		else if (idx == DrumParam.Param2)
			setWobble(knob * 0.01f);
		else if (idx == DrumParam.Param3)
			setDensity(Constants.inverseLog(knob));
	}

	@Override public int get(DrumParam idx) {
		if (idx == DrumParam.Param1)
			return shimmer;
		else if (idx == DrumParam.Param2)
			return (int) (wobble * 100f);
		else if (idx == DrumParam.Param3)
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

	private void update() {
		if (!dirty)
			return;
		transientFrames = Math.max(1, (attack * SR) / 1000);
		invTransient = 1.0f / transientFrames;

		final float bodyRef = SETTINGS.freqs().body().hz();
		final float hzInfluence = (bodyRef - 4500f) * 0.2f;

		final float hiCutHz = Math.max(200f, hiCutBase + hzInfluence);
		final float midCutHz = Math.max(100f, midCutBase + hzInfluence);

		hiAlphaCached = (float)Math.exp(-2.0 * Math.PI * hiCutHz / SR_OVER);
		midAlphaCached = (float)Math.exp(-2.0 * Math.PI * midCutHz / SR_OVER);

		final float wobbleRateHz = 6.0f * (0.2f + wobble * 1.8f);
		wobbleIncCached = wobbleRateHz * ISR_OVER;

		dirty = false;
	}

	@Override
	public void trigger(int data2) {
		super.trigger(data2);
		int blendSamples = Math.max(1, Math.round(0.005f * SR_OVER));
		wobbleLFO.trigger(blendSamples);
	}

	@Override
	protected void generate() {
		update();

		final int factor = FACTOR;
		int localChokeLeft = chokeFramesLeft;
		int localChokeTotal = chokeTotalFrames;

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

				filtHiState = filtHiState * hiAlphaCached + raw * (1.0f - hiAlphaCached);
				filtMidState = filtMidState * midAlphaCached + raw * (1.0f - midAlphaCached);

				float wob = 1.0f - wobble * 0.35f + 0.35f * lookup.forPhase(wobbleLFO.next(wobbleIncCached));

				float gate = Math.abs(raw) < (1.0f - density) ? 0f : 1f;

				float shaped = filtMidState * 0.6f + filtHiState * 0.9f;

				work[idx++] = shaped * trans * chokeMul * wob * gate * density * 0.8f;
			}
		}

		chokeFramesLeft = localChokeLeft;
		if (localChokeLeft <= 0)
			chokeTotalFrames = 0;
	}

	public void resetPhase() {
		wobbleLFO.reset();
	}

	protected void choke() {
		int frames = Math.max(1, (int)(0.03f * SR));
		if (frames > N_FRAMES)
			frames = N_FRAMES;
		chokeTotalFrames = frames;
		chokeFramesLeft = frames;
	}
}
