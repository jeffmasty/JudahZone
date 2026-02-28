package net.judah.drums.fm;

import java.util.ArrayList;
import java.util.Collections;

import javax.swing.JComboBox;

import judahzone.util.Constants;
import net.judah.drums.fm.FMSetup.OpSetup;
/** FM ratio dropdown populated from MUL (0..15) and DT2 (0..3) table
 * (https://gist.github.com/bryc/e997954473940ad97a825da4e7a496fa)
 * 	- Interesting Ratios mod -> car
	1:1 default
	2:1 octave up
	3:2 perfect fifth
	5:1 bright, metalic
	1.41:1 inharmonic, bell-like
	3.33:1 gong, percussion
	0.5:1 sub-octave
 */
public class FMRatio {

	public static enum Mode {
		RATIO_DECIMAL, // standard ratio mode: frequency is voice fundamental multiplied by ratio (e.g. 2.0 for octave up, 0.5 for octave down)
//		YAMAHA_DX, // Yamaha-style pre-computed(RATIO_DECIMAL) (https://gist.github.com/bryc/e997954473940ad97a825da4e7a496fa)
//		SEMITONE, // +/- semitones.cents (e.g. -12.0 for octave down, +7.0 for perfect fifth up)
//		ABS_HZ // absolute frequency in Hz (ignoring voice fundamental and ratio/fixed settings)
	}

    public static record RatioSpec(float ratio, String label) {
        @Override public final String toString() {
            return label;
        }
    }
	public static final RatioSpec DEFAULT = new RatioSpec(1f, "1:1");
	private static final ArrayList<RatioSpec> LUT = new ArrayList<>();
	static {

		ArrayList<Double> unsorted = new ArrayList<>();
		unsorted.add(0.25); // double down
		double[] DT2TAB = {0.0 / 768.0, 384.0 / 768.0, 500.0 / 768.0, 608.0 / 768.0};
        for (int mul = 0; mul <= 15; mul++) {
            double d = (mul == 0) ? 0.5 : mul;
            for (int dt2 = 0; dt2 <= 3; dt2++) {
                double ratio = Math.pow(2.0, DT2TAB[dt2]) * d;
                unsorted.add(ratio);
            }
        }
        Collections.sort(unsorted);
        for (Double r : unsorted)
			LUT.add(new RatioSpec(r.floatValue(), String.format("%.2f", r)));
        LUT.set(5, DEFAULT);
	}
	public static int length() {
		return LUT.size();
	}
	public static RatioSpec get(int idx) {
		return LUT.get(idx);
	}

	@Override
	public String toString() {
		return "" + spec;
	}

	public static RatioSpec lookup(float nearest) {
		for (int i = 0; i < LUT.size(); i++)
			if (LUT.get(i).ratio >= nearest)
				return LUT.get(i);
		return LUT.getLast();
	}

	private RatioSpec spec;

	public FMRatio(float ratio) {
		this.spec = lookup(ratio);
	}

	public float get() { return spec.ratio; }
	public RatioSpec spec() { return spec; }

    public JComboBox<RatioSpec> createCombo() {
    	JComboBox<RatioSpec> result = new JComboBox<>(LUT.toArray(new RatioSpec[0]));
        // result.setSelectedIndex(4); 1:1
    	return result;
    }


    /** Compute phase increment (radians per sample) for given frequency. */
    public float phaseInc(float freqHz) {
        return freqHz * spec.ratio * Constants.I2PI_SR;
    }

    /** Resolve operator frequency in Hz.
    \* @param voiceFundamental base frequency for the voice (kit fundamental / note pitch)
    \* @param op operator spec
    \* @param midiNote incoming MIDI note (optional, used for SEMITONE mode). Pass -1 if unused.
    \* @param forceFixed if true, treat as fixed absolute pitch regardless of op.fixed (useful for kit-wide overrides)
    */
    public static float resolveHz(float voiceFundamental, OpSetup op) {
    	return voiceFundamental * op.ratio();
// 		float baseFreq = forceFixed || op.mode() == Mode.ABS_HZ ? op.ratio() : float baseFreq = voiceFundamental * op.ratio();
//    	if (op.mode() == Mode.SEMITONE && midiNote >= 0) {
//		// Apply semitone/cents offset to the base frequency.
//		double semitoneOffset = op.ratio(); // in semitones, can be fractional
//			return (float)(baseFreq * Math.pow(2.0, semitoneOffset / 12.0));
//		}
//		else if (op.mode() == Mode.RATIO_DECIMAL) {
//    		// Ratio mode: frequency is voice fundamental multiplied by ratio.
//			return baseFreq;
//		}
//		else if (op.mode() == Mode.YAMAHA_DX) {
//			// Yamaha-style pre-computed ratios: already applied in the ratio value.
//			return baseFreq;
//		}
   }
	public void set(float theta) {
		spec = lookup(theta);
	}

	public void setRatioKnob(int knob) {
		spec = LUT.get(Constants.ratio(knob, LUT.size() ));
	}
	public int getRatioKnob() {
		float mine = spec.ratio();
		int size = LUT.size();
		if (size <= 1) return 0; // degenerate LUT

		for (int i = 0; i < size; i++) {
			if (LUT.get(i).ratio() >= mine) {
				// inverse of Constants.ratio(percent, size) which uses (size-1) in the mapping.
				int knob = Math.round(100f * i / (size - 1));
				// clamp to 0..100
				if (knob < 0) knob = 0;
				if (knob > 100) knob = 100;
				return knob;
			}
		}
		return 100;
	}

}
