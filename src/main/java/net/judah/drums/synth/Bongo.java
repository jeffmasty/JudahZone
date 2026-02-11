package net.judah.drums.synth;

import java.util.function.Consumer;
import java.util.function.Supplier;

import judahzone.data.Env;
import judahzone.data.Filter;
import judahzone.data.Frequency;
import judahzone.data.Stage;
import judahzone.fx.op.NoiseGen.Colour;
import judahzone.util.Phase;
import lombok.Getter;
import net.judah.drums.DrumType;
import net.judah.drums.synth.Bongo.BongoParams;
import net.judah.drums.synth.DrumParams.DrumParam;
import net.judah.drums.synth.DrumParams.Freqs;
import net.judah.drums.synth.DrumParams.Settings;
import net.judah.midi.Actives;

/** Hand-struck drum skin: pitched body with a tap transient. */
public class Bongo extends DrumOsc implements Consumer<BongoParams>, Supplier<BongoParams> {

	public static record BongoParams(float strike, short bend, Material membrane) { }

	public static Settings SETTINGS = new Settings (
			new Stage(0.35f, 50),
			new Env(10, 21),
			new Freqs( 	new Filter(180, 9f),
						new Filter(620, 0f),
						new Filter(2000, 3f)),
			Colour.VELVET,
			new String[] { "Strike", "Bend", "Head" });

	private float strike = 0.15f;
	private short bend = -2;
	@Getter private Material membrane = Material.Wood;

	private final Phase bodyPhase = new Phase();
	private final Phase tapPhase = new Phase();

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

	public Bongo(Actives actives) {
		super(DrumType.Bongo, SETTINGS, actives);
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

	@Override public int get(DrumParam idx) {
		if (idx == DrumParam.Param1)
			return (int)(strike * 100f);
		else if (idx == DrumParam.Param2)
			return (int) (((bend + 11f) / 22f) * 100f);
		else if (idx == DrumParam.Param3)
			return switch(membrane) {
				case Hide -> 1;
				case Metal -> 33;
				case Wood -> 66;
				case Plastic -> 100;
			};
		return 0;
	}

	@Override public void set(DrumParam idx, int knob) {
		if (idx == DrumParam.Param1)
			setStrike(knob * 0.01f);
		else if (idx == DrumParam.Param2) {
			float normalized = knob / 100f;
			float bend = normalized * 22f - 11f;
			setBend((short) bend);
		} else if (idx == DrumParam.Param3) {
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

		atkFrames = Math.max(1, (attack * SR) / 1000);
		dkFrames = Math.max(0, (decay * SR) / 1000);
		decayStart = atkFrames;
		decayEnd = decayStart + dkFrames;

		tapFrames = Math.max(1, (int) (atkFrames * 0.5f));
		invTapFrames = 1.0f / tapFrames;

		startFreq = pitch.getFrequency();
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
	public void trigger(int data2) {
		update();
		super.trigger(data2);
		int blendSamples = Math.max(1, Math.round(0.005f * SR));
		bodyPhase.trigger(blendSamples);
		tapPhase.trigger(blendSamples);
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

			float body = lookup.forPhase(bodyPhase.next(currentFreq * ISR));
			float tap = lookup.forPhase(tapPhase.next(tapPhaseIncCached)) * tapAmount;
			float tapNoise = noiseGen.next() * 0.18f * tapAmount;

			work[i] = body * (1.0f - strike * 0.3f) * bodyMix + (tap * tapMix + tapNoise) * strike;
		}
	}

	public void resetPhase() {
		bodyPhase.reset();
		tapPhase.reset();
	}
}
