package net.judah.drums.pluck;

import java.util.function.Supplier;

import javax.sound.midi.ShortMessage;

import judahzone.api.Frequency;
import judahzone.api.RTNotes;
import judahzone.data.Letter;
import judahzone.filter.BandPass;
import judahzone.fx.op.Comb;
import judahzone.fx.op.Noise;
import judahzone.prism.PrismRT;
import judahzone.util.Constants;
import judahzone.util.Ramp;
import lombok.Getter;
import lombok.Setter;
import net.judah.drums.Drum;
import net.judah.drums.DrumSetup;
import net.judah.drums.pluck.PluckSetup.Pick;

/** A Karplus-Strong oscillator: noise excitation → fractional delay with feedback loop.<br/>

	A compact Karplus‑Strong style plucked-string engine and family of percussive stringy
	noise instruments. Core idea: excite a short delay line with noise, run it in a feedback
	loop with a damping filter and optional taps to shape timbre. */
@PrismRT
public class PluckOsc extends Drum implements Supplier<Pick> {

	private static final int RAMP_LEN = 64;
	public static final float BW = 2.2f;
	public static final float MAX_DAMP = 0.7f;  // fine-tune
	public static final float MIN_DAMP = 0.1f;  // fine-tune
	public static final float MAX_FB = 0.999f;
	public static final float MIN_FB = 0.85f;
	public static final float MIN_BURST_MS = 1;
	public static final float MAX_BURST_MS = 30;
	public static final float MAX_COMBS = 0.84f;
    public static final float MIN_FUNDAMENTAL = 30f; // adjust as needed
	public static final float MIN_PICKUP = MIN_FUNDAMENTAL * 2;
	public static final float MAX_PICKUP = 6000;

	@Getter private final Noise noise = new Noise();
	@Getter private final PluckDelay loop;
	@Getter private final BandPass pickup;

	@Getter private int burst;
	@Setter @Getter private float amp = 1f;
	@Getter private float feedback;
	@Getter private float damp;

	private static final int COMB_COUNT = 3;
	private final Comb[] metallic = new Comb[COMB_COUNT];
	@Getter private float combs = 0.5f;

	private final float[] exciteIn;
	private final float[] exciteOut;
	private float exciteLP = 0.25f;
	private float exciteLPState = 0f;

	private final Ramp pitchRamp = new Ramp(RAMP_LEN);
	private final Ramp loopRamp = new Ramp(RAMP_LEN);
	private final Ramp fbRamp = new Ramp(RAMP_LEN);
	private final Ramp combsRamp = new Ramp(RAMP_LEN); // avoid disconinuity

	private float pitchCurrent;
	private int pluckCountdown = 0;

	public PluckOsc(DrumSetup std, Pick setup, RTNotes actives) {
		super(std, actives);

        int minFrames = (int) Math.ceil(SR / MIN_FUNDAMENTAL);
        int bufLen = Constants.nextPow2(Math.max(minFrames, N_FRAMES));
        loop = new PluckDelay(bufLen);
        pickup = new BandPass(setup.tap(), BW);
		burst = Letter.msToSamples(setup.burst());
		noise.setColour(setup.colour());
		setAmp(setup.amp());
		setFeedback(setup.feedback());
		setDamp(setup.bright());
		setRoom(setup.room());
		setCombs(setup.combs());

		this.exciteIn = new float[work.length];
		this.exciteOut = new float[work.length];

		int[] sizes = new int[] { 73, 91, 107, 119 };
		for (int i = 0; i < COMB_COUNT; i++) {
			metallic[i] = new Comb(sizes[i]);
			metallic[i].setdamp(0.5f); // TODO parameterize
			metallic[i].setFeedback(combs);
			env.addPhase(metallic[i]);
		}

		pitchRamp.reset(fundamental);
		pitchCurrent = pitchRamp.get();
		loopRamp.reset(feedback);
		fbRamp.reset(damp);
		combsRamp.reset(combs);
	}

	@Override public Pick get() {
		return new Pick(
				burst * Letter.NISR,
				amp, pickup.getFrequency(),
				feedback, damp,
				room, combs,
				noise.getColour());
	}

	@PrismRT @Override
	public void processImpl() {
		final int len = work.length;

		// build excitation buffer (pluck burst)
		for (int i = 0; i < len; i++) {
			if (pluckCountdown > 0) {
				exciteIn[i] = noise.next() * 0.5f;
				pluckCountdown--;
			} else {
				exciteIn[i] = 0f;
			}
		}

		// Advance combsRamp across this block so it progresses at audio rate.
		// Step the ramp len times (one per sample) then use the final value
		// as the feedback for the comb chain for this block.
		float rampedCombs = combsRamp.get();
		for (int i = 0; i < len; i++)
			rampedCombs = combsRamp.next();

		// process metallic combs with a smoothed feedback value
		float[] in = exciteIn;
		float[] out = exciteOut;
		for (Comb c : metallic) {
			// update comb feedback smoothly (Comb::setFeedback is cheap)
			c.setFeedback(rampedCombs);
			for (int i = 0; i < len; i++)
				out[i] = 0f;
			c.processMix(in, out);
			float[] tmp = in;
			in = out;
			out = tmp;
		}
		if (in != exciteIn) {
			for (int i = 0; i < len; i++) exciteIn[i] = in[i];
		}

		// excite LP smoothing
		float s = exciteLPState;
		float a = exciteLP;
		for (int i = 0; i < len; i++) {
			float x = exciteIn[i];
			s += (x - s) * a;
			exciteIn[i] = s;
		}
		exciteLPState = s;

		// main per-sample loop: pitch ramp, smoothed loopGain and fbCut are used
		for (int i = 0; i < len; i++) {
			pitchCurrent = pitchRamp.next();
			loop.setFrequency(pitchCurrent);
			float excitation = exciteIn[i];

			// obtain smoothed parameters per-sample (RT-safe)
			float loopGain = loopRamp.next();
			float fbCut = fbRamp.next();

			// If a reset was requested, perform the reset only after the loopGain
			// has ramped to (near) zero. This avoids abrupt energy changes/clicks.
			if (dirty) {
				// when ramp completes (loopGain reached target 0), reset and ramp back
				if (!loopRamp.isRamping() && loopGain <= 1e-6f) {
					loop.reset();                 // RT-safe zeroing (loopGain is ~0)
					loopRamp.set(this.feedback);   // ramp back to the desired feedback
					dirty = false;
					// refresh loopGain for current sample after scheduling ramp back up
					loopGain = loopRamp.next();
				}
			}
			work[i] = loop.process(excitation, loopGain, fbCut) * amp;
		}
		pickup.process(work);

	}

	public void setCombs(float fb) {
		this.combs = Math.max(0f, Math.min(1f, fb));
		// smooth comb changes via ramp rather than immediate set
		combsRamp.set(this.combs);
	}

	public void setDamp(float fbCut) {
		damp = Math.max(MIN_DAMP, Math.min(fbCut, MAX_DAMP));
		fbRamp.set(this.damp);
	}

	public void setFeedback(float loopGain) {
		feedback = Math.max(MIN_FB, Math.min(loopGain, MAX_FB));
		loopRamp.set(this.feedback);
	}

	public void setBurstKnob(int knob) {
		burst = Letter.msToSamples(MIN_BURST_MS + (MAX_BURST_MS - MIN_BURST_MS) * (knob * 0.01f));
	}
	public int getBurstKnob() {
		return (int)(((Letter.samplesToMs(burst) - MIN_BURST_MS) / (MAX_BURST_MS - MIN_BURST_MS)) * 100f);
	}

	@Override
	public void set(int idx, int knob) {
		switch(idx) {
			case 0: setBurstKnob(knob); break;
			case 1: setAmp(knob * 0.02f); break;
			case 2: pickup.setFrequency(Constants.logarithmic(knob, MIN_PICKUP, MAX_PICKUP)); break;
			case 3: setFeedback(Constants.logarithmic(knob, MIN_FB, MAX_FB)); break;
			case 4: setDamp(Constants.fromKnob(knob, MIN_DAMP, MAX_DAMP)); break;
			case 5: setRoom(knob); break;
			case 6: setCombs(Constants.fromKnob(knob, 0, MAX_COMBS)); break;
			case 7: noise.setColourKnob(knob);break;
		}
	}

	@Override
	public int get(int idx) {
		switch(idx) {
			case 0: return getBurstKnob();
			case 1: return (int)(amp * 50f);
			case 2: return Constants.reverseLog(pickup.getFrequency(), MIN_PICKUP, MAX_PICKUP);
			case 3: return Constants.reverseLog(feedback, MIN_FB, MAX_FB);
			case 4: return Constants.toKnob(damp, MIN_DAMP, MAX_DAMP);
			case 5: return room;
			case 6: return Constants.toKnob(combs, 0, MAX_COMBS);
			case 7: return noise.getColourKnob();
		}
		return -1;
	}

	@Override
	public String val(int idx) {
		switch(idx) {
		case 0: return "" + Letter.samplesToMs(burst);
		case 1: return "" + get(1);
		case 2: return Frequency.toNote(pickup.getFrequency()).toString();
		case 3: return "" + get(3);
		case 4: return "" + get(4);
		case 5: return "" + get(5);
		case 6: return "" + get(6);
		case 7: return noise.getColour().name();
		}
		return "";
	}

	@Override
	public void trigger(ShortMessage midi) {
		if (tail) {
			dirty = true; // re-trigger
			loopRamp.set(0f);    // start ramping down loop gain; actual reset happens in processImpl
		}
		super.trigger(midi);
		pluckCountdown = burst;
		pitchRamp.set(fundamental);
	}

	@Override
	public void setFundamental(float hz) {
		super.setFundamental(hz);
		pitchRamp.set(fundamental);
	}

}
/*
 * 	"Karplus-Strong discrete loop" (1983)
 *
 *	Let N = delay length (samples), g = loop gain (0..1), x[n] = initial excitation buffer.
 *
 *	For n >= N:  y[n] = g * ( y[n - N] + y[n - N - 1] ) / 2
 */

