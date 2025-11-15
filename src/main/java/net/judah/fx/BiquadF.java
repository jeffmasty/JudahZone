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

import net.judah.fx.StereoBiquad.BWQType;
import net.judah.fx.StereoBiquad.FilterType;
import net.judah.util.Constants;

public class BiquadF {
	static final float LOG_2  = 0.693147f;
	private static final int N_FRAMES = Constants.bufSize();
	private static final float SAMPLE_RATE = Constants.sampleRate();
	public static final float MAX_WIDTH = 5f;

	public static float gainDb(int val) {
	    float result = Math.abs(50 - val) / 2f;
	    if (val < 50) result *= -1;
	    return result;
	}

	protected float frequency;
	protected float bandwidth;
	protected float gain_db = 0;
	protected /* final */ FilterType filter_type;
	protected BWQType bwq_type = BWQType.BW;

	private	float a0, a1, a2, b0, b1, b2;
	private float xn1, xn2, yn1, yn2 = 0;

	public BiquadF(FilterType type, float frequency) { // 16Db Hi/Lo Pass,
		this(type, frequency, 2, 16f);
	}

	public BiquadF(int hz, float bandwidth) { // EQ left ch
		this(FilterType.Peaking, hz, bandwidth, 0f);
	}

	public BiquadF(BiquadF copy) { // EQ right ch
		this(copy.filter_type, copy.frequency, copy.bandwidth, copy.gain_db);
	}

	public BiquadF(FilterType type, float frequency, float bandwidth, float gain) {
		this.frequency = frequency;
		this.filter_type = type;
		this.bandwidth = bandwidth;
		this.gain_db = gain;
		update();
	}

	void update() {
		float a = (float)(Math.pow(10.0, gain_db/40.0));
		if (filter_type == FilterType.Gain) {
			b0 = a;
			a0 = 1.0f;
			a1 = a2 = b1 = b2 = 0.0f;
			return;
		}
		float w0 = (float)(2.0*Math.PI*frequency/SAMPLE_RATE);
		float sinw0 = (float)Math.sin(w0);
		float cosw0 = (float)Math.cos(w0);
		float alpha = 0f;
		if (bwq_type==BWQType.Q) {
			alpha = (float)(sinw0/(2.0*bandwidth));
		} else if (bwq_type==BWQType.BW) {
			alpha = (float)(sinw0*Math.sinh(LOG_2/2.0*bandwidth*w0/Math.sin(w0)));
		} else if (bwq_type==BWQType.S) {
			alpha = (float)(sinw0 * Math.sqrt((a+1.0/a)*(1/bandwidth-1)+2) / 2.0);
		}
		if (filter_type==FilterType.LowPass) {
			b1 = 1.0f - cosw0;
			b0 = b2 = b1/2.0f;
			a0 = 1.0f + alpha;
			a1 = -2.0f*cosw0;
			a2 = 1.0f - alpha;
		} else if (filter_type==FilterType.HighPass) {
			b0 = b2 = (1.0f + cosw0)/2;
			b1 = -(1.0f + cosw0);
			a0 = 1.0f + alpha;
			a1 = -2.0f * (float)Math.cos(w0);
			a2 = 1.0f - alpha;
		} else if (filter_type==FilterType.Peaking) {
			b0 = 1.0f + alpha * a;
			b1 = -2.0f*cosw0;
			b2 = 1.0f - alpha*a;
			a0 = 1.0f + alpha/a;
			a1 = -2.0f * cosw0;
			a2 = 1.0f - alpha/a;
		}
	};

	void processBuffer(FloatBuffer buff) {
		buff.rewind();
		for (int i=0; i<N_FRAMES; i++) {
			float xn = buff.get(i); // copy
			float yn = (b0*xn + b1*xn1 + b2*xn2 - a1*yn1 - a2*yn2) / a0;
            if (Math.abs(yn) < 1.0E-8)
                yn = 0; // de-normalize
			buff.put(yn); // yn, our target, back into the buffer
			xn2 = xn1;
			xn1 = xn;
			yn2 = yn1;
			yn1 = yn;
		}
	}
}
