package net.judah.drums.noise;

import java.util.function.Consumer;
import java.util.function.Supplier;

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
import net.judah.drums.noise.Stick.StickParams;

/** Filtered Noise Burst: Short, snappy stick WoodBlock style hit: click transient + pitched body.
*
*  Hybrid additive model: two pitched sine bodies (one detuned) + a fixed-frequency sine "click" transient + filtered noise for click texture.
*  Envelope: attack/decay frames computed once on parameter change; click length is fractional (~20%) of the attack.
*  Pitch bend: body frequency glides during decay via a linear/linearized slope (computed per-buffer).
*  Strike Parameter: crossfades between non‑pitched transient (click+noise) and pitched body (higher strike → stronger click, reduced body).
*  Phase: supports retrigger phase blending to avoid clicks.
*  Output scaling: separates pitched vs atonal components and applies tonal/atonal multipliers before final attenuation (0.9f).
*/
public class Stick extends NoiseOsc implements Consumer<StickParams>, Supplier<StickParams> {
	public static record StickParams(float strike, int bend, int room) { }
	public static final float DEFAULT_HZ = 500f;

	public static DrumSetup SETTINGS = new DrumSetup(DrumType.Stick,
			new GainT(0.55f), new Postage(9, 21),
			new Freqs(	new Coord(150, 1f),
						new FilterT(DEFAULT_HZ, 0f, 0.5f),
						new Coord(6666, 1f)),
			new Hz(DEFAULT_HZ),
			new String[] { "Strike", "Bend", "Room" });

	private float strike = 0.33f; // click or tap quality (0..1) (hoist final)
	private short bend = 6; // relative pitch bend semitones (+/- 24)

	private final Phase bodyPhase      = new Phase(PHASE_BLEND); // primary pitched body
	private final Phase detunePhase    = new Phase(PHASE_BLEND); // detuned secondary body
	private final Phase clickPhase     = new Phase(PHASE_BLEND); // click/transient oscillator

	// Precomputed class-level constants for audio thread (no allocations)
	private final float clickFreq = 300f;
	private final float clickPhaseInc = clickFreq * ISR;
	private final float bodyDetuneRatio = 1.5f;

	// Cached / dirty pattern
	private int clickFrames;
	private float invClickFrames;
	private int atkFrames;
	private int dkFrames;
	private int decayStart;
	private int decayEnd;
	private float linearInc;

	public Stick(RTNotes actives) {
		super(DrumType.Stick, SETTINGS, Colour.GREY, actives);
		setRoom(room);

		env.addPhase(bodyPhase);
		env.addPhase(detunePhase);
		env.addPhase(clickPhase);
	}

	@Override
	public void accept(StickParams t) {
		setStrike(t.strike());
		setBend((short) t.bend());
		setRoom(t.room());
	}

	@Override
	public StickParams get() {
		return new StickParams(strike, bend, room);
	}

	@Override public void set(int idx, int knob) {
		float val = knob * 0.01f;
		if (idx == 0)
			setStrike(val);
		else if (idx == 1) {
			// 0 to 100 +/- 24
			float bend = val * 48f - 24f;
			setBend((short) bend);
		} else if (idx == 2)
			setRoom(knob);
	}

	@Override public int get(int idx) {
		if (idx == 0)
			return (int) (strike * 100f);
		else if (idx == 1)
			return (int) ((bend + 24) * (100f / 48f));
		else if (idx == 2)
			return room;
		return 0;
	}

	public void setStrike(float s) {
		strike = s;
	}

	public void setBend(short b) {
		bend = b;
		dirty = true;
	}

	private void update() {
		if (!dirty)
			return;
		atkFrames = Math.max(1, env.getAttackSamples());
		dkFrames = Math.max(0, env.getDecaySamples());
		decayStart = atkFrames;
		decayEnd = decayStart + dkFrames;

		clickFrames = Math.max(1, (int) (atkFrames * 0.2f));
		invClickFrames = 1.0f / clickFrames;

		linearInc = dkFrames > 0 ? (1.0f / dkFrames) : 0f;

		dirty = false;
	}

	@Override
	protected void generate() {
		update();

		final float startFreq = fundamental;
		final float endFreq = startFreq * (float) Math.pow(2.0, bend * 0.083333f); // semitone bend
		final float freqSlope = linearInc * (endFreq - startFreq);
		final float localStrike = strike;

		float currentFreq = startFreq;

		for (int i = 0; i < N_FRAMES; i++) {
			final float clickAmount = i < clickFrames ? 1.0f - i * invClickFrames : 0f;

			if (i >= decayStart && i < decayEnd) {
				currentFreq += freqSlope;
			} else if (i >= decayEnd) {
				currentFreq = endFreq;
			} else {
				currentFreq = startFreq;
			}

			// compute per-sample phase increments
			final float phaseInc = currentFreq * ISR;
			final float detuneInc = phaseInc * bodyDetuneRatio;

			// Use lookup table for phase->sine via Phase objects (retrig-safe)
			float body = Phase.sin(bodyPhase.next(phaseInc));
			float body2 = Phase.sin(detunePhase.next(detuneInc));
			float noise = noiseGen.next();

			float click = Phase.sin(clickPhase.next(clickPhaseInc)) * clickAmount;
			float clickNoise = noise * 0.15f * clickAmount;

			// Separate pitched and non-pitched components for tonal/atonal scaling
			float pitchedPart = (body * (1.0f - localStrike * 0.5f)) + (body2 * 0.3f);
			float nonPitched = (click + clickNoise) * localStrike;

			// Apply tonal/atonal multipliers: tonal affects pitched body,
			// atonal affects click/noise transient
			float out = (pitchedPart * tonal) + (nonPitched * atonal);

			work[i] = out * 0.9f;
		}
	}

}
