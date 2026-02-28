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
import lombok.Getter;
import net.judah.drums.Drama.Freqs;
import net.judah.drums.DrumSetup;
import net.judah.drums.DrumType;
import net.judah.drums.noise.Bongo.BongoParams;

/** Hand-struck drum skin: pitched body with a tap transient. */
public class Bongo extends NoiseOsc implements Consumer<BongoParams>, Supplier<BongoParams> {

	public static record BongoParams(float strike, short bend, Material membrane) { }
	public static final float DEFAULT_HZ = 300f;
	public static DrumSetup SETTINGS = new DrumSetup ( DrumType.Bongo,
			new GainT(0.35f), new Postage(15, 200),
			new Freqs( 	new Coord(180, 9f),
						new FilterT(new Coord(DEFAULT_HZ, 0f)),
						new Coord(2000, 3f)),
			new Hz(DEFAULT_HZ),
			new String[] { "Strike", "Bend", "Head" });

	private float strike = 0.6f;
	private short bend = -2;
	@Getter private Material membrane = Material.Wood;

	private final Phase bodyPhase = new Phase(PHASE_BLEND);
	private final Phase tapPhase = new Phase(PHASE_BLEND);

	private int atkFrames;
	private int dkFrames;
	private int decayStart;
	private int decayEnd;
	private int tapFrames;
	private float invTapFrames;
	private float startFreq;
	private float freqMulPerFrame;
	private float endFreq;
	private float tapPhaseIncCached;
	private float bodyMix;
	private float tapMix;

	public Bongo(RTNotes actives) {
		super(DrumType.Bongo, SETTINGS, Colour.VELVET, actives);

		env.addPhase(bodyPhase);
		env.addPhase(tapPhase);
	}

	@Override
	public void accept(BongoParams t) {
		setStrike(t.strike());
		setBend(t.bend());
		setMembrane(t.membrane());
	}

	@Override public BongoParams get() {
		return new BongoParams(strike, bend, membrane);
	}

	@Override
	public String val(int idx) {
		if (idx == 0)
			return "" + (int)(strike * 100);
		else if (idx == 1)
			return Short.toString(bend);
		else if (idx == 2)
			return membrane.name();
		return "";
	}

	@Override public int get(int idx) {
		if (idx == 0)
			return (int)(strike * 100f);
		else if (idx == 1)
			return (int) (((bend + 11f) / 22f) * 100f);
		else if (idx == 2)
			return switch(membrane) {
				case Hide -> 1;
				case Metal -> 33;
				case Wood -> 66;
				case Plastic -> 100;
			};
		return 0;
	}

	@Override public void set(int idx, int knob) {
		if (idx == 0)
			setStrike(knob * 0.01f);
		else if (idx == 1) {
			float normalized = knob / 100f;
			float bend = normalized * 22f - 11f;
			setBend((short) bend);
		} else if (idx == 2) {
			Material m = switch(knob) {
				case 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25
					-> Material.Hide;
				case 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50
					-> Material.Metal;
				case 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75
					-> Material.Wood;
				default -> Material.Plastic;
			};
			setMembrane(m);
		}
	}

	public void setStrike(float strike) {
		this.strike = Math.max(0f, Math.min(1f, strike));
		dirty = true;
	}

	public void setBend(short bend) {
		this.bend = (short) Math.max(-11, Math.min(11, bend));
		dirty = true;
	}

	public void setMembrane(Material membrane) {
		if (membrane == null)
			return;
		this.membrane = membrane;
		dirty = true;
	}

	private void update() {
		if (!dirty)
			return;

		atkFrames = Math.max(1, env.getAttackSamples());
		dkFrames = Math.max(0, env.getDecaySamples());
		decayStart = atkFrames;
		decayEnd = decayStart + dkFrames;

		tapFrames = Math.max(1, (int) (atkFrames * 0.5f));
		invTapFrames = 1.0f / tapFrames;

		startFreq = fundamental;
		int midi = Frequency.hzToMidi(startFreq) + bend;
		endFreq = Frequency.midiToHz(midi);

		if (dkFrames > 0) {
			float ratio = endFreq / startFreq;
			freqMulPerFrame = (float) Math.pow(ratio, 1.0f / dkFrames);
		} else {
			freqMulPerFrame = 1.0f;
		}

		float tapFreqLocal;
		switch (membrane) {
		case Metal -> {
			tapFreqLocal = 420f;
			tapMix = 0.9f;
			bodyMix = 0.4f;
		}
		case Wood -> {
			tapFreqLocal = 280f;
			tapMix = 0.75f;
			bodyMix = 0.95f;
		}
		case Plastic -> {
			tapFreqLocal = 200f;
			tapMix = 0.85f;
			bodyMix = 0.8f;
		}
		default /* Hide */ -> {
			tapFreqLocal = 110f;
			tapMix = 0.6f;
			bodyMix = 1.0f;
		}
		}
		tapPhaseIncCached = tapFreqLocal * ISR;

		dirty = false;
	}

	@Override
	public void trigger(ShortMessage midi) {
		update();
		super.trigger(midi);
	}

	@Override
	protected void generate() {
		update();

		float currentFreq = startFreq;

		for (int i = 0; i < N_FRAMES; i++) {
			final float tapAmount = i < tapFrames ? 1.0f - i * invTapFrames : 0f;

			if (i < decayEnd && dkFrames > 0)
				currentFreq = startFreq * (float) Math.pow(freqMulPerFrame, i);
			else if (i >= decayEnd)
				currentFreq = endFreq;

			float body = Phase.sin(bodyPhase.next(currentFreq * ISR));
			float tap = Phase.sin(tapPhase.next(tapPhaseIncCached)) * tapAmount;
			float tapNoise = noiseGen.next() * 0.18f * tapAmount;

			work[i] = body * (1.0f - strike * 0.3f) * bodyMix + (tap * tapMix + tapNoise) * strike;
		}
	}

}
