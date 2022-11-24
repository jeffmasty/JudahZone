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
package net.judah.effects;

import static java.lang.Math.*;

import java.nio.FloatBuffer;
import java.security.InvalidParameterException;

import lombok.Getter;
import lombok.Setter;
import net.judah.effects.api.Effect;
import net.judah.util.Constants;

public class Compressor implements Effect {

    public static enum Settings { 
    	Threshold, Ratio, Boost, Attack, Release, Knee
    }

    static final float LOG_10 = 2.302585f;
	static final float LOG_2  = 0.693147f;
	static final float MIN_GAIN  = 0.00001f; // -100dB help prevents evaluation of denormal numbers
    @Setter @Getter private boolean active;
    @Getter private int preset;

    // private int hold = (int) (samplerate*0.0125);  //12.5ms;
    private double cSAMPLE_RATE;

    private float lvolume = 0.0f;
    private float lvolume_db = 0.0f;
    private int tratio = 4;
    private int toutput = -10;
    private int tknee = 30;
    private float boost = 1.0f;
    private float boost_old = 1.0f;
    private float gain_t = 1.0f;
    private double ratio = 1.0;
    private float kpct = 0.0f;

    private float peak = 0.0f;
    private float rell = 1f;
    private float attl = 1f;

    private float thres_db = -24;
    private float att;
    private int attStash;
    private float rel;
    private int relStash;

    private double eratio;
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

    /** initializes Preset[1] at 48000 sampleRate */
    public Compressor() {
    	setSampleRate(Constants.sampleRate());
    	setThreshold(-25);
    	setRatio(8);
		setBoost(-17);
		setKnee(20);
    	setAttack(30);
		setRelease(220);
    }
    public void setSampleRate(int sampleRate) {
    	cSAMPLE_RATE = 1.0/sampleRate;
    }

    @Override
    public int getParamCount() {
        return Settings.values().length;
    }

    @Override public String getName() {
        return Compressor.class.getSimpleName();
    }

	public float dB2rap(double dB) {
		return (float)((Math.exp((dB)*LOG_10/20.0f)));
	}

	public float rap2dB(float rap) {
		return (float)((20*log(rap)/LOG_10));
	}

	public void reset() {
	    boost = 1.0f;
	    boost_old = 1.0f;
	    peak = 0.0f;
	    compute();
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
    	att = attl = (float) (cSAMPLE_RATE /((attStash / 1000.0f) + cSAMPLE_RATE));
    	compute();
    }

    public void setRelease(int milliseconds) {
    	relStash = milliseconds;
    	rel = (float) (cSAMPLE_RATE /((milliseconds / 1000.0f) + cSAMPLE_RATE));
    	rell = rel;
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
        	return (getThreshold() * -3) + 1; 
        if (idx == Settings.Ratio.ordinal())
        	return getRatio() * 10 - 2;
        if (idx == Settings.Attack.ordinal())
        	return getAttack() + 10;
        if (idx == Settings.Release.ordinal())
        	return (int)( (getRelease() - 20) * 0.5f);
        throw new InvalidParameterException();
    }

	@Override
	public void set(int idx, int value) {
		if (idx == Settings.Threshold.ordinal())
			setThreshold((int)(value * -0.333f) - 1) ;
		else if (idx == Settings.Ratio.ordinal()) 
			setRatio((int)((value * 0.1f) + 2));
		else if (idx == Settings.Boost.ordinal()) 
			setBoost(value); // non-conform 1 to 100
		else if (idx == Settings.Attack.ordinal()) 
			setAttack(value + 10);
		else if (idx == Settings.Release.ordinal()) 
			setRelease( (value + 20) * 2);
		else if (idx == Settings.Knee.ordinal()) 
			setKnee(value); // non-conform 1 to 100
		
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
	
	
	public void process(FloatBuffer buf) {
		buf.rewind();
		float val;
	    for (int z = 0; z < buf.capacity(); z++) {
	    	val = buf.get(z);
	        float ldelta = 0.0f;
	        peak = val;

  	        //Mono Channel
	        ldelta = abs (peak);

	        if(lvolume < 0.9f) {
	            attl = att;
	            rell = rel;
	        } else if (lvolume < 1.0f) {
	            attl = att + ((1.0f - att)*(lvolume - 0.9f)*10.0f);	//dynamically change attack time for limiting mode
	            rell = rel/(1.0f + (lvolume - 0.9f)*9.0f);  //release time gets longer when signal is above limiting
	        } else {
	            attl = 1.0f;
	            rell = rel*0.1f;
	        }

	        if (ldelta > lvolume)
	            lvolume = attl * ldelta + (1.0f - attl)*lvolume;
	        else
	            lvolume = rell*ldelta + (1.0f - rell)*lvolume;

	        lvolume_db = rap2dB (lvolume);

	        if (lvolume_db < thres_db) {
	            boost = outlevel;
	        } else if (lvolume_db < thres_mx) { //knee region
	            eratio = 1.0f + (kratio-1.0f)*(lvolume_db-thres_db)* coeff_knee;
	            boost =   outlevel*dB2rap(thres_db + (lvolume_db-thres_db)/eratio - lvolume_db);
	        } else {
	            boost = outlevel*dB2rap(thres_db + coeff_kk + (lvolume_db-thres_mx)*coeff_ratio - lvolume_db);
	        }

	        if ( boost < MIN_GAIN) boost = MIN_GAIN;
	        gain_t = .4f * boost + .6f * boost_old;
            buf.put(val * gain_t);
            boost_old = boost;
	    }
	}

}
