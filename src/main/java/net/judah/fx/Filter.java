package net.judah.fx;

/*https://github.com/adiblol/jackiir*/
/*
jackiir
IIR parametric equalizer for JACK without GUI

Copyright (C) 2014 adiblol

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

import java.nio.FloatBuffer;
import java.security.InvalidParameterException;

import judahzone.api.Effect.RTEffect;
import judahzone.util.Constants;
import lombok.Getter;
import net.judah.fx.StereoBiquad.FilterType;

public class Filter implements RTEffect {

    public enum Settings { Type, Hz, Width, dB }

    public static final int MIN = 45;
    public static final int MAX = 13500;

    @Getter private final String name = Filter.class.getSimpleName();
    @Getter private final int paramCount = Settings.values().length;

    private final StereoBiquad filter;

//	 /*filter_type can be: g (gain without EQ), pk (peaking), hp/lc (highpass/lowcut), lp/hc (lowpass/highcut)
//	 # freq is center frequency (for peaking) or cut/boost frequency
//	 # bandwidth: 1 = 1 octave
//	 lc 20 60 0 # remove DC and subsonic
//	 pk 80 60 +6 # boost lows
//	 pk 8000 90 +9 # boost highs */
//	float hz = type == FilterType.HighPass ? 100 : 4000;
    public Filter(boolean lowPass) {
		float hz = lowPass ? MAX : MIN;
		FilterType type = lowPass ? FilterType.LowPass : FilterType.HighPass;
		filter = new StereoBiquad(type, hz);
    }

	@Override public void set(int idx, int value) {
		if (idx == Settings.dB.ordinal())
			filter.gain_db = StereoBiquad.gainDb(value);
		else if (idx == Settings.Hz.ordinal())
			filter.frequency = Constants.logarithmic(value, MIN, MAX);
		else if (idx == Settings.Width.ordinal())
			filter.bandwidth = StereoBiquad.MAX_WIDTH * value * 0.01f;
		else if (idx == Settings.Type.ordinal()) { // "LOCUT" CC81
			FilterType change = value > 50 ? FilterType.HighPass : FilterType.LowPass;
			filter.filter_type = change;
		}
		else throw new InvalidParameterException("" + idx);
		filter.coefficients();
	}

	@Override public int get(int idx) {
		if (idx == Settings.dB.ordinal())
			return Math.round(filter.gain_db * 2 + 50);
		else if (idx == Settings.Hz.ordinal())
			return Constants.reverseLog(filter.frequency, MIN, MAX);
		else if (idx == Settings.Width.ordinal()) // x/MAX_WIDTH = y/100
			return (int) (filter.bandwidth * 100 / StereoBiquad.MAX_WIDTH);
		else if (idx == Settings.Type.ordinal())
			return filter.filter_type == FilterType.HighPass ? 100 : 0;
		throw new InvalidParameterException("" + idx);
	}

	@Override public void process(FloatBuffer left, FloatBuffer right) {
		filter.process(left, right);
	}

}
