package net.judah.mixer.bus;

import java.nio.FloatBuffer;

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


public class EQ {
	public static final int CHANNELS_MAX = 32;
	public static final int FILTERS_MAX = 32;
	static final float LOG_2  = 0.693147f;
	
	final int samplerate = 48000; // roll your own
	final int nframes = 512; // also
	
	private boolean active;
	public void setActive(boolean active) {
		this.active = active;
	}
	public boolean isActive() {
		return active;
	}
	
	public static enum EqParam {
		GAIN, FREQUENCY, BANDWIDTH
	}
	
	public static enum EqBand {
		BASS, MID, TREBLE
	}
	
	public static enum FilterType {
		Gain, LowPass, HighPass, Peaking
	};
	
	public static enum BWQType {
		Q, BW, S
	};

	class Channel {
		BiquadFilter[] filters = new BiquadFilter[EqBand.values().length];
	}
	private final Channel leftCh = new Channel();
	private final Channel rightCh = new Channel();

	class BiquadFilter {
		
		private	float a0, a1, a2, b0, b1, b2;
		private float xn1, xn2, yn1, yn2 = 0;
		
		float frequency; 
		float bandwidth;
		float gain_db = 0; 
		final FilterType filter_type;
		BWQType bwq_type = BWQType.BW;
		
		public BiquadFilter(float frequency, float bandwidth, FilterType type) {
			this.frequency = frequency;
			this.filter_type = type;
			this.bandwidth = bandwidth;
			update();
		}
		
		public BiquadFilter(BiquadFilter copy) {
			this(copy.frequency, copy.bandwidth, copy.filter_type);
		}

		void update() {
			float a = (float)(Math.pow(10.0, gain_db/40.0));
			if (filter_type == FilterType.Gain) {
				b0 = a;
				a0 = 1.0f;
				a1 = a2 = b1 = b2 = 0.0f;
				return;
			}
			float w0 = (float)(2.0*Math.PI*frequency/samplerate);
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
			} else if (filter_type==FilterType.Gain) {
			}
		};
		
		void processBuffer(FloatBuffer buff) {
			buff.rewind();
			for (int i=0; i<nframes; i++) {
				float xn = buff.get(i); // copy
				float yn = (b0*xn + b1*xn1 + b2*xn2 - a1*yn1 - a2*yn2) / a0;
				buff.put(yn); // yn, our target, back into the buffer
				xn2 = xn1;
				xn1 = xn;
				yn2 = yn1;
				yn1 = yn;
			}
		}
		void processBuffer(float[] buff) {
			for (int i=0; i<nframes; i++) {
				float xn = buff[i]; // copy
				float yn = (b0*xn + b1*xn1 + b2*xn2 - a1*yn1 - a2*yn2) / a0;
				buff[i] = yn; // yn, our target, back into the buffer
				xn2 = xn1;
				xn1 = xn;
				yn2 = yn1;
				yn1 = yn;
			}
		};
	}
	
	public EQ() {
		 /*f <channels_separated_by_commas> <filter_type> <freq> <bandwidth> <gain>
		 # filter_type can be: g (gain without EQ), pk (peaking), hp/lc (highpass/lowcut), lp/hc (lowpass/highcut)
		 # freq is center frequency (for peaking) or cut/boost frequency
		 # bandwidth: 1 = 1 octave
		 # gain is in dB
		 f 1,2 g 0 0 -9 # attenuate before boosting to prevent clipping
		 f 1,2 lc 20 60 0 # remove DC and subsonic
		 f 1,2 pk 80 60 +6 # boost lows
		 #f 1,2 pk 8000 90 +9 # boost highs */
		BiquadFilter bass = new BiquadFilter(120, 1.1f, FilterType.Peaking);
		leftCh.filters[EqBand.BASS.ordinal()] = bass;
		rightCh.filters[EqBand.BASS.ordinal()] = new BiquadFilter(bass);
		
		BiquadFilter mid = new BiquadFilter(666, 1.2f, FilterType.Peaking);
		leftCh.filters[EqBand.MID.ordinal()] = mid;
		rightCh.filters[EqBand.MID.ordinal()] = new BiquadFilter(mid);
		
		BiquadFilter treble = new BiquadFilter(3100, 1.7f, FilterType.Peaking);
		leftCh.filters[EqBand.TREBLE.ordinal()] = treble;
		rightCh.filters[EqBand.TREBLE.ordinal()] = new BiquadFilter(treble);
	}
	
	public void update(EqBand band, EqParam param, float value) {
		BiquadFilter filter = leftCh.filters[band.ordinal()];
		switch (param) {
			case BANDWIDTH: filter.bandwidth = value; break;
			case FREQUENCY: filter.frequency = value; break;
			case GAIN: filter.gain_db = value; break;
		}
		filter.update();
		
		filter = rightCh.filters[band.ordinal()];
		switch (param) {
			case BANDWIDTH: filter.bandwidth = value; break;
			case FREQUENCY: filter.frequency = value; break;
			case GAIN: filter.gain_db = value; break;
		}
		filter.update();
	}
	
	public void process(FloatBuffer data, boolean left) {
		if (left)
			for (BiquadFilter filter : leftCh.filters) 
				filter.processBuffer(data);
		else
			for (BiquadFilter filter : rightCh.filters) 
				filter.processBuffer(data);
		
	}
	public void process(float[] data, boolean left) {
		// TODO watch for Java's handling of denormals/flush-to-zero/etc...
		
		// start filtering:
		if (left)
			for (BiquadFilter filter : leftCh.filters) 
				filter.processBuffer(data);
		else
			for (BiquadFilter filter : rightCh.filters) 
				filter.processBuffer(data);
	 }
	 
}
