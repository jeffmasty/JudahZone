package net.judah.mixer;

import static java.lang.Math.*;

import java.nio.FloatBuffer;

import lombok.Getter;
import lombok.Setter;
import net.judah.util.Console;
import net.judah.util.Constants;

public class Compression {

    static final float LOG_10 = 2.302585f;
	static final float LOG_2  = 0.693147f;
	static final float MIN_GAIN  = 0.00001f;        // -100dB  This will help prevent evaluation of denormal numbers

	/** number of compression presets, default = 1 */
	public static final int COMPRESSION_PRESETS = 3;
	private static final int PRESET_SIZE = 7;
    private static int[][] presets = new int[][] { // TODO make enum
        /* 2:1 */ {-30, 2, -13, 20, 120, 0},
        /* 4:1 */ {-26, 4, -17, 30, 270, 10},
        /* 8:1 */ {-24, 8, -18, 20, 35, 30} };

    @Setter @Getter private boolean active; 
    @Getter private int preset = -1;
    
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
    public Compression() {
    	setSampleRate(Constants._SAMPLERATE);
    	setPreset(1);
    }
    public void setSampleRate(int sampleRate) {
    	cSAMPLE_RATE = 1.0/sampleRate;
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

    /** Compression preset 0 (2:1) , 1 (4:1), or 2 (8:1) */
	public void setPreset (int preset) {
		this.preset = preset;
        for (int n = 1; n < PRESET_SIZE; n++)
            setPreset(n , presets[preset][n-1]);
	}

	public void incrementPreset() {
		//boolean active = channel.getCompression().isActive();
		if (!isActive()) {
			setPreset(0);
			setActive(true);
		}
		else {
			int preset = 1 + getPreset();
			if (preset < Compression.COMPRESSION_PRESETS)
				setPreset(preset);
			else
				setActive(false);
		}
		Console.info("Compression on: " + isActive() + " / " + getPreset());
	}
	
    /** @return attack in milliseconds */
    public int getAttack() {
    	return attStash;
    }
    
    public void setAttack(int milliseconds) {
    	setPreset(4, milliseconds);
    }
    
    public void setRelease(int milliseconds) {
    	setPreset(5, milliseconds);
    }
    
    public int getRatio() {
    	return tratio;
    }
    public float getThreshold() {
    	return thres_db;
    }
    
	public void setRatio(int val) {
		setPreset(2, val);
	}
	public void setThreshold(int val) {
		setPreset(1, val); 
	}

	public void setPreset(int np, int value) {
	    switch (np) {
	    case 1:
	        thres_db = value;    //implicit type cast int to float
	        break;
	    case 2:
	        tratio = value;
	        ratio = tratio;
	        break;
	    case 3:
	        toutput = value;
	        break;
	    case 4:
	    	attStash = value;
	        att = (float) (cSAMPLE_RATE /((value / 1000.0f) + cSAMPLE_RATE));
	        attl = att;
	        break;
	    case 5:
	    	relStash = value;
	        rel = (float) (cSAMPLE_RATE /((value / 1000.0f) + cSAMPLE_RATE));
	        rell = rel;
	        break;
	    case 6:
	        tknee = value;  //knee expressed a percentage of range between thresh and zero dB
	        kpct = tknee/100.1f;
	        break;
	    }

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
	
	public void process(float[] in, FloatBuffer out, float gain) {
		float val;
	    for (int z = 0; z < out.capacity(); z++) {
	    	val = in[z] * gain;
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
