/* ported from https://github.com/ssj71/rkrlv2/blob/master/src/Compressor.C */
/*
  rakarrack - a guitar effects software
 Compressor.C  -  Compressor Effect
 Based on artscompressor.cc by Matthias Kretz <kretz@kde.org>
 Stefan Westerfeld <stefan@space.twc.de>
  Copyright (C) 2008-2010 Josep Andreu
  Author: Josep Andreu
	Patches:
	September 2009  Ryan Billing (a.k.a. Transmogrifox)
		--Modified DSP code to fix discontinuous gain change at threshold.
		--Improved automatic gain adjustment function
		--Improved handling of knee
		--Added support for user-adjustable knee
		--See inline comments
 This program is free software; you can redistribute it and/or modify
 it under the terms of version 2 of the GNU General Public License
 as published by the Free Software Foundation.
 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License (version 2) for more details.
 You should have received a copy of the GNU General Public License
 (version2)  along with this program; if not, write to the Free Software
 Foundation,
 Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307 USA
*/
package net.judah.fx;

import static java.lang.Math.abs;
import static java.lang.Math.log;

import java.nio.FloatBuffer;
import java.security.InvalidParameterException;

import lombok.Getter;
import lombok.Setter;

public class Compressor implements Effect {

    public static enum Settings {
    	Threshold, Ratio, Boost, Attack, Release, Knee
    }

    static final float LOG_10 = 2.302585f;
	static final float LOG_2  = 0.693147f;
	static final float MIN_GAIN  = 0.00001f; // -100dB help prevents evaluation of denormal numbers
    @Setter @Getter private boolean active;

    // private int hold = (int) (samplerate*0.0125);  //12.5ms;
    private final double cSAMPLE_RATE = 1.0/SAMPLE_RATE;;

    private float lvolume = 0.0f;
    private int tratio = 4;
    private int toutput = -10;
    private int tknee = 30;
    private float boost_old = 1.0f;
    private double ratio = 1.0;
    private float kpct = 0.0f;

    private float thres_db = -24;
    private float att;
    private int attStash;
    private float rel;
    private int relStash;

    private double kratio;
    private float knee;
    private double coeff_kratio;
    private double coeff_ratio;
    private double coeff_knee;
    private double coeff_kk;
    private float thres_mx;
    private double makeup;
    private float makeuplin;
    private float outlevel;

    public Compressor() {
    	reset();
    }

	public void reset() {
	    boost_old = 1.0f;
    	setThreshold(-16);
    	setRatio(7);
		setBoost(-14);
		setKnee(20);
		setRelease(90);
		set(Settings.Attack.ordinal(), get(Settings.Release.ordinal()));
	}

    public static float dB2rap(double dB) {
		return (float)((Math.exp((dB)*LOG_10/20.0f)));
	}

	public static float rap2dB(float rap) {
		return (float)((20*log(rap)/LOG_10));
	}

    @Override
    public int getParamCount() {
        return Settings.values().length;
    }

    @Override public String getName() {
        return Compressor.class.getSimpleName();
    }

    /** @return attack in milliseconds */
    public int getAttack() {
    	return attStash;
    }

    /** @return release in milliseconds */
    public int getRelease() {
    	return relStash;
    }

    public int getRatio() {
    	return tratio;
    }
    public int getThreshold() {
    	return Math.round(thres_db);
    }

	public void setRatio(int val) {
		tratio = val;
		ratio = tratio;
		compute();
	}
	public void setThreshold(int db) {
		thres_db = db;
		compute();
	}
    public void setAttack(int milliseconds) {
    	attStash = milliseconds;
    	att = (float) (cSAMPLE_RATE /((attStash / 1000.0f) + cSAMPLE_RATE));
    	compute();
    }

    public void setRelease(int milliseconds) {
    	relStash = milliseconds;
    	rel = (float) (cSAMPLE_RATE /((relStash / 1000.0f) + cSAMPLE_RATE));
    	compute();
    }

    public void setKnee(int knee) {
    	tknee = knee;  //knee expressed a percentage of range between thresh and zero dB
    	kpct = tknee/100.1f;
    	compute();
    }

    public void setBoost(int boost) {
    	toutput = boost;
    	compute();
    }

    @Override
    public int get(int idx) {
        if (idx == Settings.Threshold.ordinal())
        	return (getThreshold() * -3) - 1;
        if (idx == Settings.Ratio.ordinal())
        	return (getRatio() - 2) * 10;
        	// return getRatio() * 10 - 2;
        if (idx == Settings.Attack.ordinal())
        	return getAttack() - 10;
        if (idx == Settings.Release.ordinal())
        	return (int) ((getRelease() -5) * 0.5f);
        if (idx == Settings.Boost.ordinal())
        	return (toutput + 20) * 3;
        if (idx == Settings.Knee.ordinal())
        	return tknee;
        throw new InvalidParameterException("idx: " + idx);
    }
	@Override
	public void set(int idx, int value) {
		if (idx == Settings.Threshold.ordinal())
			setThreshold((int)(value * -0.333f) - 1) ;
		else if (idx == Settings.Ratio.ordinal())
			setRatio(2 + (int)(value * 0.1f));
		else if (idx == Settings.Boost.ordinal())
			setBoost((int) Math.floor(value  * 0.333334f - 20));
			// setBoost(value); // non-conform 1 to 100
		else if (idx == Settings.Attack.ordinal())
			setAttack(value + 10);
		else if (idx == Settings.Release.ordinal())
			setRelease( (value * 2) + 5);
		else if (idx == Settings.Knee.ordinal())
			setKnee(value);
		else throw new InvalidParameterException("Compressor set " + idx + "? val: " + value);

	}

	private void compute() {
	    kratio = Math.log(ratio)/ LOG_2;  //  Log base 2 relationship matches slope
	    knee = -kpct*thres_db;

	    coeff_kratio = 1.0 / kratio;
	    coeff_ratio = 1.0 / ratio;
	    coeff_knee = 1.0 / knee;
	    coeff_kk = knee * coeff_kratio;

	    thres_mx = thres_db + knee;  //This is the value of the input when the output is at t+k
	    makeup = -thres_db - knee/kratio + thres_mx/ratio;
	    makeuplin = dB2rap(makeup);
        outlevel = dB2rap(toutput) * makeuplin;
	}

	@Override
	public void process(FloatBuffer left, FloatBuffer right) {
		process(left);
		process(right);
	}

	void process(FloatBuffer buf) {
		float val, ldelta, attl, rell, lvolume_db, gain_t, boost;
		double eratio;
		final float lvol = lvolume;
		final float outl = outlevel;
		final float threshold = thres_db;
		buf.rewind();
	    for (int z = 0; z < buf.capacity(); z++) {
	    	val = buf.get(z);
	        ldelta = abs (val);

	        if (lvol < 0.9f) {
	            attl = att;
	            rell = rel;
	        } else if (lvol < 1f) {
	            attl = att + ((1f - att) * (lvol - 0.9f) * 10.0f); //dynamically change attack time for limiting mode
	            rell = rel / (1f + (lvol - 0.9f) * 9.0f);  //release time gets longer when signal is above limiting
	        } else {
	            attl = 1f;
	            rell = rel * 0.1f;
	        }

	        if (ldelta > lvol)
	            lvolume = attl * ldelta + (1f - attl) * lvol;
	        else
	            lvolume = rell * ldelta + (1f - rell) * lvol;

	        lvolume_db = rap2dB (lvolume);

	        if (lvolume_db < threshold)
	            boost = outl;
	        else if (lvolume_db < thres_mx) { //knee region
	            eratio = 1f + (kratio - 1f) * (lvolume_db-threshold) * coeff_knee;
	            boost =   outl * dB2rap(threshold + (lvolume_db-threshold) / eratio - lvolume_db);
	        } else
	            boost = outl * dB2rap(threshold + coeff_kk + (lvolume_db-thres_mx) * coeff_ratio - lvolume_db);
	        if (boost < MIN_GAIN)
	        	boost = MIN_GAIN;

	        gain_t = .4f * boost + .6f * boost_old;
            buf.put(val * gain_t);
            boost_old = boost;
	    }
	}

}
