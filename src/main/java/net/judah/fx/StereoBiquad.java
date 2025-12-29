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

import judahzone.util.Constants;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** compute unit of and EQ and Filter */
public class StereoBiquad {

	@RequiredArgsConstructor @Getter
	public static enum FilterType {
		LowPass("HiCut"), HighPass("LoCut"), Peaking("EQ");
		final String display;}
	public static enum BWQType { Q, BW, S }

	public static final float LOG_2  = 0.693147f;
	static final float MAX_WIDTH = 5f;
	private static final int N_FRAMES = Constants.bufSize();
	private static final float SAMPLE_RATE = Constants.sampleRate();

	protected float frequency;
	protected float bandwidth;
	protected float gain_db = 0;
	protected FilterType filter_type;
	protected BWQType bwq_type = BWQType.BW;
	private final Biquad left, right;

	// current coefficients
	private float a0, a1, a2, b0, b1, b2;
	// previous coefficients for smoothing
	private float lastA0, lastA1, lastA2, lastB0, lastB1, lastB2;
	private boolean coeffDirty = true;
	private boolean haveLastCoeffs = false;

	public StereoBiquad(FilterType type, float frequency) { // Hi/Lo pass
		this(type, frequency, 2, 16f);
	}

	public StereoBiquad(int hz) {
		this(hz, 1.5f);
	}

	public StereoBiquad(int hz, float bandwidth) { // EQ
		this(FilterType.Peaking, hz, bandwidth, 0f);
	}

	public StereoBiquad(FilterType type, float frequency, float bandwidth, float gain) {
		this.frequency = frequency;
		this.filter_type = type;
		this.bandwidth = bandwidth;
		this.gain_db = gain;
		left = new Biquad();
		right = new Biquad();
		coefficients();
	}

	public void coefficients() {
		float a = (float)(Math.pow(10.0, gain_db/40.0));
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
		coeffDirty = true;
	}

	public static float gainDb(int val) {
	    float result = Math.abs(50 - val) / 2f;
	    if (val < 50) result *= -1;
	    return result;
	}

	public void process(FloatBuffer l, FloatBuffer r) {
		// snapshot current coeffs into Biquads with smoothing across this block
		left.updateCoefficients();
		right.updateCoefficients();
		left.processBuffer(l);
		right.processBuffer(r);
	}

	private class Biquad {

		private float xn1, xn2, yn1, yn2 = 0;

		void updateCoefficients() {
			// If coefficients changed since last time, set up interpolation
			if (coeffDirty || !haveLastCoeffs) {
				// if we have previous coefficients, keep them for smoothing
				if (!haveLastCoeffs) {
					lastA0 = a0;
					lastA1 = a1;
					lastA2 = a2;
					lastB0 = b0;
					lastB1 = b1;
					lastB2 = b2;
					haveLastCoeffs = true;
				}
				coeffDirty = false;
			}
		}

		void processBuffer(FloatBuffer buff) {
			buff.rewind();

			// If we don't have previous coefficients yet, just use current ones, no smoothing
			if (!haveLastCoeffs ||
			    (lastA0 == a0 && lastA1 == a1 && lastA2 == a2 &&
			     lastB0 == b0 && lastB1 == b1 && lastB2 == b2)) {

				final float lb0 = b0;
				final float lb1 = b1;
				final float lb2 = b2;
				final float la0 = a0;
				final float la1 = a1;
				final float la2 = a2;

				for (int i = 0; i < N_FRAMES; i++) {
					float xn = buff.get(i);
					float yn = (lb0 * xn + lb1 * xn1 + lb2 * xn2
					            - la1 * yn1 - la2 * yn2) / la0;
					if (Math.abs(yn) < 1.0E-8f)
						yn = 0f; // de-normalize
					buff.put(i, yn);
					xn2 = xn1;
					xn1 = xn;
					yn2 = yn1;
					yn1 = yn;
				}

			} else {
				// Smoothly interpolate coefficients from lastA* / lastB* to a* / b* over this buffer
				float curA0 = lastA0;
				float curA1 = lastA1;
				float curA2 = lastA2;
				float curB0 = lastB0;
				float curB1 = lastB1;
				float curB2 = lastB2;

				final float dA0 = (a0 - lastA0) / N_FRAMES;
				final float dA1 = (a1 - lastA1) / N_FRAMES;
				final float dA2 = (a2 - lastA2) / N_FRAMES;
				final float dB0 = (b0 - lastB0) / N_FRAMES;
				final float dB1 = (b1 - lastB1) / N_FRAMES;
				final float dB2 = (b2 - lastB2) / N_FRAMES;

				for (int i = 0; i < N_FRAMES; i++) {
					curA0 += dA0;
					curA1 += dA1;
					curA2 += dA2;
					curB0 += dB0;
					curB1 += dB1;
					curB2 += dB2;

					float xn = buff.get(i);
					float yn = (curB0 * xn + curB1 * xn1 + curB2 * xn2
					            - curA1 * yn1 - curA2 * yn2) / curA0;
					if (Math.abs(yn) < 1.0E-8f)
						yn = 0f;
					buff.put(i, yn);
					xn2 = xn1;
					xn1 = xn;
					yn2 = yn1;
					yn1 = yn;
				}
			}

			// At the end of this block, current coeffs become "last" for the next one
			lastA0 = a0;
			lastA1 = a1;
			lastA2 = a2;
			lastB0 = b0;
			lastB1 = b1;
			lastB2 = b2;
		}
	}
}