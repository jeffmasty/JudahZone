package net.judah.fx;

import java.nio.FloatBuffer;
import java.security.InvalidParameterException;

import lombok.Getter;
import lombok.Setter;
import net.judah.fx.StereoBiquad.FilterType;
import net.judah.util.Constants;

public class Filter implements Effect {

    public enum Settings { Type, Hz, Width, dB }

    public static final int MIN = 40;
    public static final int MAX = 14400;

    @Getter private final String name = Filter.class.getSimpleName();
    @Getter private final int paramCount = Settings.values().length;

    @Setter @Getter private boolean active;
    private final BiquadF rightCh;
    private final BiquadF leftCh;

//	 /*filter_type can be: g (gain without EQ), pk (peaking), hp/lc (highpass/lowcut), lp/hc (lowpass/highcut)
//	 # freq is center frequency (for peaking) or cut/boost frequency
//	 # bandwidth: 1 = 1 octave
//	 lc 20 60 0 # remove DC and subsonic
//	 pk 80 60 +6 # boost lows
//	 pk 8000 90 +9 # boost highs */
//	float hz = type == FilterType.HighPass ? 100 : 4000;
    public Filter(boolean lowPass) {
		float hz = lowPass ? MAX : MIN;
		leftCh = new BiquadF(lowPass ? FilterType.LowPass : FilterType.HighPass, hz);
		rightCh = new BiquadF(leftCh);

		leftCh.update();
		rightCh.update();
    }

	@Override public void set(int idx, int value) {
		if (idx == Settings.dB.ordinal())
			leftCh.gain_db = rightCh.gain_db = BiquadF.gainDb(value);
		else if (idx == Settings.Hz.ordinal())
			leftCh.frequency = rightCh.frequency = Constants.logarithmic(value, MIN, MAX);
		else if (idx == Settings.Width.ordinal())
			leftCh.bandwidth = rightCh.bandwidth = BiquadF.MAX_WIDTH * value * 0.01f;
		else if (idx == Settings.Type.ordinal()) { // "LOCUT" CC81
			FilterType change = value > 50 ? FilterType.HighPass : FilterType.LowPass;
			leftCh.filter_type = rightCh.filter_type = change;
		}
		else throw new InvalidParameterException("" + idx);
		leftCh.update();
		rightCh.update();
	}

	@Override public int get(int idx) {
		if (idx == Settings.dB.ordinal())
			return Math.round(leftCh.gain_db * 2 + 50);
		else if (idx == Settings.Hz.ordinal())
			return Constants.reverseLog(leftCh.frequency, MIN, MAX);
		else if (idx == Settings.Width.ordinal()) // x/MAX_WIDTH = y/100
			return (int) (leftCh.bandwidth * 100 / BiquadF.MAX_WIDTH);
		else if (idx == Settings.Type.ordinal())
			return leftCh.filter_type == FilterType.HighPass ? 100 : 0;
		throw new InvalidParameterException("" + idx);
	}

	@Override public void process(FloatBuffer left, FloatBuffer right) {
		leftCh.processBuffer(left);
		rightCh.processBuffer(right);
	}

}
