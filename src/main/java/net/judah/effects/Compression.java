package net.judah.effects;

import static java.lang.Math.abs;
import static java.lang.Math.log;

import java.nio.FloatBuffer;
import java.security.InvalidParameterException;

import lombok.Getter;
import lombok.Setter;
import net.judah.effects.api.Effect;
import net.judah.util.Constants;

public class Compression implements Effect {

    public enum Settings {
        Threshold, Attack, Release
    }

    static final float LOG_10 = 2.302585f;
	static final float LOG_2  = 0.693147f;
	static final float MIN_GAIN  = 0.00001f; // -100dB help prevents evaluation of denormal numbers

    @Setter @Getter private boolean active;

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
    /** computed attack value */
    private float att;
    /** attack represented in milliseconds (0-150)*/
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

    public Compression() {
    	setSampleRate(Constants.sampleRate());
    	setThreshold(-26);
    	setRatio(6);
    	setOutput(-6);
    	setAttack(20);
    	setRelease(60);
    	setKnee(30);
    }
    
    public void setSampleRate(int sampleRate) {
    	cSAMPLE_RATE = 1.0/sampleRate;
    }

    @Override
    public int getParamCount() {
        return Settings.values().length;
    }

    @Override public String getName() {
        return Compression.class.getSimpleName();
    }

    @Override
    public int get(int idx) {
        if (idx == Settings.Threshold.ordinal())
            return (int)((getThreshold() + 40) * 2.5f);
        if (idx == Settings.Attack.ordinal()) {
        	int attack = Math.round(getAttack() * 0.66666f);
        	return attack > 100 ? 100 : attack;
        }
        if (idx == Settings.Release.ordinal()) {
        	int release = (int)(getRelease() * 0.33333f);
        	return release > 100 ? 100 : release;
        }
        throw new InvalidParameterException();
    }

    @Override
    public void set(int idx, int value) {
        if (idx == Settings.Threshold.ordinal())
            setThreshold((value - 100) / 2.5f);
        else if (idx == Settings.Attack.ordinal())
            setAttack((int)(value * 1.5f));
        else if (idx == Settings.Release.ordinal())
            setRelease(Math.round(value * 3));
        else throw new InvalidParameterException();
    }

    /** @return release in milliseconds */
    public int getRelease() {
    	return relStash;
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
	}


    /**@return attack in milliseconds */
    public int getAttack() {
    	return attStash;
    }

    public int getRatio() {
    	return tratio;
    }
    public float getThreshold() {
    	return thres_db;
    }

	public void setRatio(int val) {
		ratio = tratio = val;
		compute();
	}
	
	public void setThreshold(float val) {
		thres_db = val;
		compute();
	}

	public void setOutput(int val) {
		toutput = val;
		compute();
	}
	
	public void setKnee(int val) {
		tknee = val;  //knee expressed a percentage of range between thresh and zero dB
	    kpct = tknee/100.1f;
	    compute();
	}
	
	public void setAttack(int milliseconds) {
		attStash = milliseconds;
        att = (float) (cSAMPLE_RATE /((milliseconds / 1000.0f) + cSAMPLE_RATE));
        attl = att;
        compute();
    }

    public void setRelease(int milliseconds) {
    	relStash = milliseconds;
    	rel = (float) (cSAMPLE_RATE /((milliseconds / 1000.0f) + cSAMPLE_RATE));
    	rell = rel;
    	compute();
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
	

	public void process(FloatBuffer buf, float gain) {
		buf.rewind();
		float val;
	    for (int z = 0; z < buf.capacity(); z++) {
	    	val = buf.get(z) * gain;
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

	public void process(float[] in, FloatBuffer out) {
	    out.rewind();
		float val;
	    for (int z = 0; z < out.capacity(); z++) {
	    	val = in[z];
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
	        out.put(out.get(z) + (val * gain_t));
            boost_old = boost;
	    }
	}


}
