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
import java.util.ArrayList;

import lombok.Getter;
import lombok.Setter;

public class EQ implements Effect {
	public static final int CHANNELS_MAX = 32;
	public static final int FILTERS_MAX = 32;
	static final float LOG_2  = 0.693147f;

 	public static enum Settings		{ dB, Hz, Width}
	public static enum EqBand 		{ Bass, Mid, High }

	public static int[] FREQUENCIES = {90, 500, 5000};

	@Getter private final String name;
	private final ArrayList<BiquadF> leftCh = new ArrayList<BiquadF>();
	private final ArrayList<BiquadF> rightCh = new ArrayList<BiquadF>();
	@Getter @Setter private boolean active;

	public static record Frequencies(int low, int mid, int high) {};
	public static Frequencies DEFAULT = new Frequencies(90, 500, 5000);

	public EQ() {
		this(DEFAULT);
	}

	/** create 3 peak EQs of given Frequencies, each spanning more than an octave */
	public EQ(Frequencies hz) {
		this.name = EQ.class.getSimpleName();
		BiquadF bass = new BiquadF(hz.low, 1.3f);

		leftCh.add(bass);
		rightCh.add(new BiquadF(bass));

		BiquadF mid = new BiquadF(hz.mid, 1.5f);
		leftCh.add(mid);
		rightCh.add(new BiquadF(mid));

		BiquadF treble = new BiquadF(hz.high, 1.75f);
		leftCh.add(treble);
		rightCh.add(new BiquadF(treble));
	}

	private void update(BiquadF filter, Settings param, float value) {
		switch (param) {
			case dB: filter.gain_db = value; break;
			case Hz: filter.frequency = value; break;
			case Width: filter.bandwidth = value; break;
		}
		filter.update();
	}

	public void update(EqBand band, Settings param, float value) {
		update(leftCh.get(band.ordinal()), param, value);
		update(rightCh.get(band.ordinal()), param, value);
	}

	// TODO
    @Override public int getParamCount() {
        return EqBand.values().length;
    }

    // TODO support hz
    @Override public int get(int idx) {
    	float nonNormal = leftCh.get(idx).gain_db;
    	return Math.round(nonNormal * 2 + 50);
    }
    // TODO support hz
    @Override public void set(int idx, int val) {
    	EqBand band = EqBand.values()[idx];
    	eqGain(band, val);
    }


	public void eqGain(EqBand eqBand, int val) {
        update(eqBand, Settings.dB, BiquadF.gainDb(val));
	}

    public float getGain(EqBand band) {
	    return leftCh.get(band.ordinal()).gain_db;
	}

	@Override
	public void process(FloatBuffer left, FloatBuffer right) {
		for (BiquadF filter : leftCh)
			filter.processBuffer(left);
		for (BiquadF filter : rightCh)
			filter.processBuffer(right);
	}

}

