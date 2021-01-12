package net.judah.mixer.bus;

import java.nio.FloatBuffer;

/*
* Adapted from code in Gervill.
*
* Copyright 2007 Sun Microsystems, Inc.  All Rights Reserved.
* DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
*
* This code is free software; you can redistribute it and/or modify it
* under the terms of the GNU General Public License version 2 only, as
* published by the Free Software Foundation.  Sun designates this
* particular file as subject to the "Classpath" exception as provided
* by Sun in the LICENSE file that accompanied this code.
*
* This code is distributed in the hope that it will be useful, but WITHOUT
* ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
* FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
* version 2 for more details (a copy is included in the LICENSE file that
* accompanied this code).
*
* You should have received a copy of the GNU General Public License version
* 2 along with this work; if not, see http://www.gnu.org/licenses/
* 
*
* Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
* CA 95054 USA or visit www.sun.com if you need additional information or
* have any questions.
*/
/*
* Infinite impulse response (IIR) filter class.
*
* The filters where implemented and adapted using algorithms from musicdsp.org
* archive: 1-RC and C filter, Simple 2-pole LP LP and HP filter, biquad,
* tweaked butterworth RBJ Audio-EQ-Cookbook, EQ filter kookbook
*
* @author Karl Helgason
*/

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.judah.util.Constants;
import net.judah.util.RTLogger;

//https://github.com/jaudiolibs/audioops/blob/master/audioops-impl/src/main/java/org/jaudiolibs/audioops/impl/IIRFilterOp.java
/**
 * IIR Filter op ported from Gervill in OpenJDK.
 *
 * This is a mono op and requires one input and output channel. Input and output
 * buffers may be the same.
 * @author Neil C Smith (derived from code by Karl Helgason)
 */
public class CutFilter {
	// More options:
	// TODO https://github.com/adiblol/jackiir (used as java class EQ)
	// TODO https://github.com/T-5dotEU/ladspa-t5-plugins
	// TODO https://github.com/Dragerus/parametricEQ-jack
	// TODO	https://github.com/hathagat/Equalizer
	
    @RequiredArgsConstructor @Getter
    public static enum Type {
        LP6("6dB one pole low pass"), 
        LP12("12dB two pole low pass"), 
        HP12("12dB two pole high pass"), 
        BP12("12dB two pole band pass"), 
        NP12("12dB two pole notch pass"), 
        LP24("24dB four pole low pass"), 
        HP24("24dB four pole high pass"),
        pArTy("low freq low pass, high freq hi pass")
        ;
        private final String description;
    };
    private static final int PARTY_FREQUENCY = 420;
    
    /** y = 1/30 * x ^ 2.81 + bassFloor */
	public static float knobToFrequency(int val) { 
		return (float)(0.033333 * Math.pow(val, 2.81)) + 80f;
		
	}
	
	public static int frequencyToKnob(float freq) {
		//return Math.round(30 * Math.)
		return (int)Math.round(  30 * ((Math.log1p(freq))/ 2.81 * 1.97)) - 95;
	}

	public static void main2(String[] args) {
		System.out.println("knob 10 = " + knobToFrequency(10));
		System.out.println("knob 50 = " + knobToFrequency(50));
		System.out.println("knob 90 = " + knobToFrequency(90));
		
		System.out.println("91.52159hz = " + frequencyToKnob(91.52159f));
		System.out.println("2051.4363hz = " + frequencyToKnob(2051.4363f));
		System.out.println("10404.652hz = " + frequencyToKnob(10404.652f));
	}
	
    private final double sampleRate;
    private final int bufferSize;
    
    /** left channel */
    private final IIRFilter eq1 = new IIRFilter();
    /** right channel */
    private final IIRFilter eq2 = new IIRFilter();
    
    @Getter private Type filterType = Type.BP12;
    @Setter @Getter private boolean active;
    private double freqeuncy = 650.;
    private double resonancedB = 12.;
    private Type oldParty;

	public CutFilter() {
    	this(Constants._SAMPLERATE, Constants._BUFSIZE);
    }

    public CutFilter(int sampleRate, int bufferSize) {
    	this.sampleRate = sampleRate;
    	this.bufferSize = bufferSize;
    	eq1.reset();
    	eq2.reset();
    }
    
    /**Set frequency of filter in Hz. <br/>
     *  Recommended range (20 - samplerate/2) */
    public void setFrequency(float hz) {
        if (freqeuncy == hz) 
            return;
        freqeuncy = hz;
        eq1.dirty = eq2.dirty = true;
    }

    /** @return frequency of filter */
    public float getFrequency() {
        return (float) freqeuncy;
    }

    /**Set resonance of filter in dB. <br/>
     * Recommended range (0 - 30)*/
    public void setResonance(float db) {
        if (resonancedB == db) 
            return;
        resonancedB = db;
        eq1.dirty = eq2.dirty = true;
        RTLogger.log(this, "resonance: " + resonancedB);
    }

    /** @return resonance of filter. */
    public float getResonance() {
        return (float) resonancedB;
    }

    /** Set filter type. */
    public void setFilterType(Type filtertype) {
        if (filtertype == null) {
            throw new NullPointerException();
        }
        if (this.filterType == filtertype) return;
        this.filterType = filtertype;
        eq1.dirty = eq2.dirty = true;
        if (filterType == Type.pArTy) {
        	oldParty = freqeuncy <= PARTY_FREQUENCY ? Type.LP12 : Type.HP12;
        }
        RTLogger.log(this, "filter type: " + filterType);
    }
    

    /** process replace mono */
    public void process(FloatBuffer mono, float gain) {
    	mono.rewind();
        switch (filterType) {
        case LP6:
    		eq1.filter1Replace(mono, gain);
            break;
        case LP12:
        case HP12:
        case NP12:
        case BP12:
        	eq1.filter2Replace(mono, gain);
            break;
        case LP24:
        case HP24:
        	eq1.filter4Replace(mono, gain);
            break;
        case pArTy: 
        	checkParty();
        	eq1.filter2Replace(mono, gain * 2.5f);
        	break;
        }
    }
    
    /** process replace stereo */
    public void process(FloatBuffer left, FloatBuffer right, float gain) {
    	left.rewind();
    	right.rewind();
        switch (filterType) {
        case LP6:
    		eq1.filter1Replace(left, gain);
    		eq2.filter1Replace(right, gain);
            break;
        case LP12:
        case HP12:
        case NP12:
        case BP12:
        	eq1.filter2Replace(left, gain);
        	eq2.filter2Replace(right, gain);
            break;
        case LP24:
        case HP24:
        	eq1.filter4Replace(left, gain);
        	eq2.filter4Replace(right, gain);
            break;
        case pArTy: 
        	checkParty();
        	eq1.filter2Replace(left, gain * 2.5f);
        	eq2.filter2Replace(right, gain * 2.5f);
        }
    	
    }
    
    private void checkParty() {
		if (freqeuncy <= PARTY_FREQUENCY) {
			if (oldParty != Type.LP12) {
				eq1.dirty = eq2.dirty = true;
				oldParty = Type.LP12;
			}
		}
		else {
			if (oldParty != Type.HP12) {
				eq1.dirty = eq2.dirty = true;
				oldParty = Type.HP12;
			}
		}
    }
    
    
    class IIRFilter {

        private boolean dirty = true;
	    private double x1;
	    private double x2;
	    private double y1;
	    private double y2;
	    private double xx1;
	    private double xx2;
	    private double yy1;
	    private double yy2;
	    private double a0;
	    private double a1;
	    private double a2;
	    private double b1;
	    private double b2;
	    private double q;
	    private double gain = 1;
	    private double wet = 0;
	    private double last_wet = 0;
	    private double last_a0;
	    private double last_a1;
	    private double last_a2;
	    private double last_b1;
	    private double last_b2;
	    private double last_q;
	    private double last_gain;
	    private boolean last_set = false;
	
		public void reset() {
	        dirty = true;
	        last_set = false;
	        wet = 0.0f;
	        gain = 1.0f;
	
	        x1 = 0; x2 = 0;
	        y1 = 0; y2 = 0;
	        xx1 = 0; xx2 = 0;
	        yy1 = 0; yy2 = 0;
	        a0 = 0; a1 = 0;
	        a2 = 0; 
	        b1 = 0; b2 = 0;     
	    }
		
		protected void filter4Replace(FloatBuffer data, float vol) {

			if (dirty) {
	            filter2calc();
	            dirty = false;
	        }
	        if (!last_set) {
	            last_a0 = a0;
	            last_a1 = a1;
	            last_a2 = a2;
	            last_b1 = b1;
	            last_b2 = b2;
	            last_gain = gain;
	            last_wet = wet;
	            last_set = true;
	        }
	
	        if (wet > 0 || last_wet > 0) {
	
	            double _a0 = this.last_a0;
	            double _a1 = this.last_a1;
	            double _a2 = this.last_a2;
	            double _b1 = this.last_b1;
	            double _b2 = this.last_b2;
	            double _gain = this.last_gain;
	            double _wet = this.last_wet;
	            double a0_delta = (this.a0 - this.last_a0) / bufferSize;
	            double a1_delta = (this.a1 - this.last_a1) / bufferSize;
	            double a2_delta = (this.a2 - this.last_a2) / bufferSize;
	            double b1_delta = (this.b1 - this.last_b1) / bufferSize;
	            double b2_delta = (this.b2 - this.last_b2) / bufferSize;
	            double gain_delta = (this.gain - this.last_gain) / bufferSize;
	            double wet_delta = (this.wet - this.last_wet) / bufferSize;
	            double _x1 = this.x1;
	            double _x2 = this.x2;
	            double _y1 = this.y1;
	            double _y2 = this.y2;
	            double _xx1 = this.xx1;
	            double _xx2 = this.xx2;
	            double _yy1 = this.yy1;
	            double _yy2 = this.yy2;
	
	            if (wet_delta != 0) {
	                for (int i = 0; i < bufferSize; i++) {
	                    _a0 += a0_delta;
	                    _a1 += a1_delta;
	                    _a2 += a2_delta;
	                    _b1 += b1_delta;
	                    _b2 += b2_delta;
	                    _gain += gain_delta;
	                    _wet += wet_delta;
	                    double x = data.get(i) * vol;
	                    double y = (_a0 * x + _a1 * _x1 + _a2 * _x2 - _b1 * _y1 - _b2 * _y2);
	                    double xx = (y * _gain) * _wet + (x) * (1 - _wet);
	                    _x2 = _x1;
	                    _x1 = x;
	                    _y2 = _y1;
	                    _y1 = y;
	                    double yy = (_a0 * xx + _a1 * _xx1 + _a2 * _xx2 - _b1 * _yy1 - _b2 * _yy2);
	                    data.put( (float) ((yy * _gain) * _wet + (xx) * (1 - _wet)) );
	                    _xx2 = _xx1;
	                    _xx1 = xx;
	                    _yy2 = _yy1;
	                    _yy1 = yy;
	                }
	            } else if (a0_delta == 0 && a1_delta == 0 && a2_delta == 0 && b1_delta == 0 && b2_delta == 0) {
	                for (int i = 0; i < bufferSize; i++) {
	                    double x = data.get(i) * vol;
	                    double y = (_a0 * x + _a1 * _x1 + _a2 * _x2 - _b1 * _y1 - _b2 * _y2);
	                    double xx = (y * _gain) * _wet + (x) * (1 - _wet);
	                    _x2 = _x1;
	                    _x1 = x;
	                    _y2 = _y1;
	                    _y1 = y;
	                    double yy = (_a0 * xx + _a1 * _xx1 + _a2 * _xx2 - _b1 * _yy1 - _b2 * _yy2);
	                    data.put( (float) ((yy * _gain) * _wet + (xx) * (1 - _wet)) );
	                    _xx2 = _xx1;
	                    _xx1 = xx;
	                    _yy2 = _yy1;
	                    _yy1 = yy;
	                }
	            } else {
	                for (int i = 0; i < bufferSize; i++) {
	                    _a0 += a0_delta;
	                    _a1 += a1_delta;
	                    _a2 += a2_delta;
	                    _b1 += b1_delta;
	                    _b2 += b2_delta;
	                    _gain += gain_delta;
	                    double x = data.get(i) * vol;
	                    double y = (_a0 * x + _a1 * _x1 + _a2 * _x2 - _b1 * _y1 - _b2 * _y2);
	                    double xx = (y * _gain) * _wet + (x) * (1 - _wet);
	                    _x2 = _x1;
	                    _x1 = x;
	                    _y2 = _y1;
	                    _y1 = y;
	                    double yy = (_a0 * xx + _a1 * _xx1 + _a2 * _xx2 - _b1 * _yy1 - _b2 * _yy2);
	                    data.put( (float) ((yy * _gain) * _wet + (xx) * (1 - _wet)) );
	                    _xx2 = _xx1;
	                    _xx1 = xx;
	                    _yy2 = _yy1;
	                    _yy1 = yy;
	                }
	            }
	
	            if (Math.abs(_x1) < 1.0E-8) {
	                _x1 = 0;
	            }
	            if (Math.abs(_x2) < 1.0E-8) {
	                _x2 = 0;
	            }
	            if (Math.abs(_y1) < 1.0E-8) {
	                _y1 = 0;
	            }
	            if (Math.abs(_y2) < 1.0E-8) {
	                _y2 = 0;
	            }
	            this.x1 = _x1;
	            this.x2 = _x2;
	            this.y1 = _y1;
	            this.y2 = _y2;
	            this.xx1 = _xx1;
	            this.xx2 = _xx2;
	            this.yy1 = _yy1;
	            this.yy2 = _yy2;
	        } 
	        
	        this.last_a0 = this.a0;
	        this.last_a1 = this.a1;
	        this.last_a2 = this.a2;
	        this.last_b1 = this.b1;
	        this.last_b2 = this.b2;
	        this.last_gain = this.gain;
	        this.last_wet = this.wet;
	
	    }
	
	    protected void filter4Add(float[] in, float[] out) {
	
	        if (dirty) {
	            filter2calc();
	            dirty = false;
	        }
	        if (!last_set) {
	            last_a0 = a0;
	            last_a1 = a1;
	            last_a2 = a2;
	            last_b1 = b1;
	            last_b2 = b2;
	            last_gain = gain;
	            last_wet = wet;
	            last_set = true;
	        }
	
	        if (wet > 0 || last_wet > 0) {
	
	            double _a0 = this.last_a0;
	            double _a1 = this.last_a1;
	            double _a2 = this.last_a2;
	            double _b1 = this.last_b1;
	            double _b2 = this.last_b2;
	            double _gain = this.last_gain;
	            double _wet = this.last_wet;
	            double a0_delta = (this.a0 - this.last_a0) / bufferSize;
	            double a1_delta = (this.a1 - this.last_a1) / bufferSize;
	            double a2_delta = (this.a2 - this.last_a2) / bufferSize;
	            double b1_delta = (this.b1 - this.last_b1) / bufferSize;
	            double b2_delta = (this.b2 - this.last_b2) / bufferSize;
	            double gain_delta = (this.gain - this.last_gain) / bufferSize;
	            double wet_delta = (this.wet - this.last_wet) / bufferSize;
	            double _x1 = this.x1;
	            double _x2 = this.x2;
	            double _y1 = this.y1;
	            double _y2 = this.y2;
	            double _xx1 = this.xx1;
	            double _xx2 = this.xx2;
	            double _yy1 = this.yy1;
	            double _yy2 = this.yy2;
	
	            if (wet_delta != 0) {
	                for (int i = 0; i < bufferSize; i++) {
	                    _a0 += a0_delta;
	                    _a1 += a1_delta;
	                    _a2 += a2_delta;
	                    _b1 += b1_delta;
	                    _b2 += b2_delta;
	                    _gain += gain_delta;
	                    _wet += wet_delta;
	                    double x = in[i];
	                    double y = (_a0 * x + _a1 * _x1 + _a2 * _x2 - _b1 * _y1 - _b2 * _y2);
	                    double xx = (y * _gain) * _wet + (x) * (1 - _wet);
	                    _x2 = _x1;
	                    _x1 = x;
	                    _y2 = _y1;
	                    _y1 = y;
	                    double yy = (_a0 * xx + _a1 * _xx1 + _a2 * _xx2 - _b1 * _yy1 - _b2 * _yy2);
	                    out[i] += (yy * _gain) * _wet + (xx) * (1 - _wet);
	                    _xx2 = _xx1;
	                    _xx1 = xx;
	                    _yy2 = _yy1;
	                    _yy1 = yy;
	                }
	            } else if (a0_delta == 0 && a1_delta == 0 && a2_delta == 0 && b1_delta == 0 && b2_delta == 0) {
	                for (int i = 0; i < bufferSize; i++) {
	                    double x = in[i];
	                    double y = (_a0 * x + _a1 * _x1 + _a2 * _x2 - _b1 * _y1 - _b2 * _y2);
	                    double xx = (y * _gain) * _wet + (x) * (1 - _wet);
	                    _x2 = _x1;
	                    _x1 = x;
	                    _y2 = _y1;
	                    _y1 = y;
	                    double yy = (_a0 * xx + _a1 * _xx1 + _a2 * _xx2 - _b1 * _yy1 - _b2 * _yy2);
	                    out[i] += (yy * _gain) * _wet + (xx) * (1 - _wet);
	                    _xx2 = _xx1;
	                    _xx1 = xx;
	                    _yy2 = _yy1;
	                    _yy1 = yy;
	                }
	            } else {
	                for (int i = 0; i < bufferSize; i++) {
	                    _a0 += a0_delta;
	                    _a1 += a1_delta;
	                    _a2 += a2_delta;
	                    _b1 += b1_delta;
	                    _b2 += b2_delta;
	                    _gain += gain_delta;
	                    double x = in[i];
	                    double y = (_a0 * x + _a1 * _x1 + _a2 * _x2 - _b1 * _y1 - _b2 * _y2);
	                    double xx = (y * _gain) * _wet + (x) * (1 - _wet);
	                    _x2 = _x1;
	                    _x1 = x;
	                    _y2 = _y1;
	                    _y1 = y;
	                    double yy = (_a0 * xx + _a1 * _xx1 + _a2 * _xx2 - _b1 * _yy1 - _b2 * _yy2);
	                    out[i] += (yy * _gain) * _wet + (xx) * (1 - _wet);
	                    _xx2 = _xx1;
	                    _xx1 = xx;
	                    _yy2 = _yy1;
	                    _yy1 = yy;
	                }
	            }
	
	            if (Math.abs(_x1) < 1.0E-8) {
	                _x1 = 0;
	            }
	            if (Math.abs(_x2) < 1.0E-8) {
	                _x2 = 0;
	            }
	            if (Math.abs(_y1) < 1.0E-8) {
	                _y1 = 0;
	            }
	            if (Math.abs(_y2) < 1.0E-8) {
	                _y2 = 0;
	            }
	            this.x1 = _x1;
	            this.x2 = _x2;
	            this.y1 = _y1;
	            this.y2 = _y2;
	            this.xx1 = _xx1;
	            this.xx2 = _xx2;
	            this.yy1 = _yy1;
	            this.yy2 = _yy2;
	        } else {
	            for (int i = 0; i < bufferSize; i++) {
	                out[i] += in[i];
	            }
	        }
	
	        this.last_a0 = this.a0;
	        this.last_a1 = this.a1;
	        this.last_a2 = this.a2;
	        this.last_b1 = this.b1;
	        this.last_b2 = this.b2;
	        this.last_gain = this.gain;
	        this.last_wet = this.wet;
	
	    }
	
	    private double sinh(double x) {
	        return (Math.exp(x) - Math.exp(-x)) * 0.5;
	    }
	
	    protected void filter2calc() {
	
	        double rdB = resonancedB;
	        if (rdB < 0) {
	            rdB = 0;    // Negative dB are illegal.
	        }
	        if (rdB > 30) {
	            rdB = 30;   // At least 22.5 dB is needed.
	        }
	        if (filterType == Type.LP24 || filterType == Type.HP24) {
	            rdB *= 0.6;
	        }
	
	        if (filterType == Type.BP12) {
	            wet = 1;
	            double r = (freqeuncy / sampleRate);
	            if (r > 0.45) {
	                r = 0.45;
	            }
	
	            double bandwidth = Math.PI * Math.pow(10.0, -(rdB / 20));
	
	            double omega = 2 * Math.PI * r;
	            double cs = Math.cos(omega);
	            double sn = Math.sin(omega);
	            double alpha = sn * sinh((Math.log(2) * bandwidth * omega) / (sn * 2));
	
	            double _b0 = alpha;
	            double _b1 = 0;
	            double _b2 = -alpha;
	            double _a0 = 1 + alpha;
	            double _a1 = -2 * cs;
	            double _a2 = 1 - alpha;
	
	            double cf = 1.0 / _a0;
	            this.b1 = (_a1 * cf);
	            this.b2 = (_a2 * cf);
	            this.a0 = (_b0 * cf);
	            this.a1 = (_b1 * cf);
	            this.a2 = (_b2 * cf);
	        }
	
	        if (filterType == Type.NP12) {
	            wet = 1;
	            double r = (freqeuncy / sampleRate);
	            if (r > 0.45) {
	                r = 0.45;
	            }
	
	            double bandwidth = Math.PI * Math.pow(10.0, -(rdB / 20));
	
	            double omega = 2 * Math.PI * r;
	            double cs = Math.cos(omega);
	            double sn = Math.sin(omega);
	            double alpha = sn * sinh((Math.log(2) * bandwidth * omega) / (sn * 2));
	
	            double _b0 = 1;
	            double _b1 = -2 * cs;
	            double _b2 = 1;
	            double _a0 = 1 + alpha;
	            double _a1 = -2 * cs;
	            double _a2 = 1 - alpha;
	
	            double cf = 1.0 / _a0;
	            this.b1 = (_a1 * cf);
	            this.b2 = (_a2 * cf);
	            this.a0 = (_b0 * cf);
	            this.a1 = (_b1 * cf);
	            this.a2 = (_b2 * cf);
	        }
	
	        if (filterType == Type.LP12 || filterType == Type.LP24 ||
	        		(filterType == Type.pArTy && freqeuncy <= PARTY_FREQUENCY)) {
	            double r = (freqeuncy / sampleRate);
	            if (r > 0.45) {
	                if (wet == 0) {
	                    if (rdB < 0.00001) {
	                        wet = 0.0f;
	                    } else {
	                        wet = 1.0f;
	                    }
	                }
	                r = 0.45;
	            } else {
	                wet = 1.0f;
	            }
	
	            double c = 1.0 / (Math.tan(Math.PI * r));
	            double csq = c * c;
	            double resonance = Math.pow(10.0, -(rdB / 20));
	            double _q = Math.sqrt(2.0f) * resonance;
	            double _a0 = 1.0 / (1.0 + (_q * c) + (csq));
	            double _a1 = 2.0 * _a0;
	            double _a2 = _a0;
	            double _b1 = (2.0 * _a0) * (1.0 - csq);
	            double _b2 = _a0 * (1.0 - (_q * c) + csq);
	
	            this.a0 = _a0;
	            this.a1 = _a1;
	            this.a2 = _a2;
	            this.b1 = _b1;
	            this.b2 = _b2;
	
	        }
	
	        if (filterType == Type.HP12 || filterType == Type.HP24 ||
	        		(filterType == Type.pArTy && freqeuncy > PARTY_FREQUENCY)) {
	            double r = (freqeuncy / sampleRate);
	            if (r > 0.45) {
	                r = 0.45;
	            }
	            if (r < 0.0001) {
	                r = 0.0001;
	            }
	            wet = 1.0f;
	            double c = (Math.tan(Math.PI * (r)));
	            double csq = c * c;
	            double resonance = Math.pow(10.0, -(rdB / 20));
	            double _q = Math.sqrt(2.0f) * resonance;
	            double _a0 = 1.0 / (1.0 + (_q * c) + (csq));
	            double _a1 = -2.0 * _a0;
	            double _a2 = _a0;
	            double _b1 = (2.0 * _a0) * (csq - 1.0);
	            double _b2 = _a0 * (1.0 - (_q * c) + csq);
	
	            this.a0 = _a0;
	            this.a1 = _a1;
	            this.a2 = _a2;
	            this.b1 = _b1;
	            this.b2 = _b2;
	
	        }
	
	    }
	
	    protected void filter2Replace(FloatBuffer data, float vol) {
	
	        if (dirty) {
	            filter2calc();
	            dirty = false;
	        }
	        if (!last_set) {
	            last_a0 = a0;
	            last_a1 = a1;
	            last_a2 = a2;
	            last_b1 = b1;
	            last_b2 = b2;
	            last_q = q;
	            last_gain = gain;
	            last_wet = wet;
	            last_set = true;
	        }
	
	        if (wet > 0 || last_wet > 0) {
	
	            double _a0 = this.last_a0;
	            double _a1 = this.last_a1;
	            double _a2 = this.last_a2;
	            double _b1 = this.last_b1;
	            double _b2 = this.last_b2;
	            double _gain = this.last_gain;
	            double _wet = this.last_wet;
	            double a0_delta = (this.a0 - this.last_a0) / bufferSize;
	            double a1_delta = (this.a1 - this.last_a1) / bufferSize;
	            double a2_delta = (this.a2 - this.last_a2) / bufferSize;
	            double b1_delta = (this.b1 - this.last_b1) / bufferSize;
	            double b2_delta = (this.b2 - this.last_b2) / bufferSize;
	            double gain_delta = (this.gain - this.last_gain) / bufferSize;
	            double wet_delta = (this.wet - this.last_wet) / bufferSize;
	            double _x1 = this.x1;
	            double _x2 = this.x2;
	            double _y1 = this.y1;
	            double _y2 = this.y2;
	
	            if (wet_delta != 0) {
	                for (int i = 0; i < bufferSize; i++) {
	                    _a0 += a0_delta;
	                    _a1 += a1_delta;
	                    _a2 += a2_delta;
	                    _b1 += b1_delta;
	                    _b2 += b2_delta;
	                    _gain += gain_delta;
	                    _wet += wet_delta;
	                    double x = data.get(i) * vol;
	                    double y = (_a0 * x + _a1 * _x1 + _a2 * _x2 - _b1 * _y1 - _b2 * _y2);
	                    data.put( (float) ((y * _gain) * _wet + (x) * (1 - _wet)) );
	                    _x2 = _x1;
	                    _x1 = x;
	                    _y2 = _y1;
	                    _y1 = y;
	                }
	            } else if (a0_delta == 0 && a1_delta == 0 && a2_delta == 0 && b1_delta == 0 && b2_delta == 0) {
	                for (int i = 0; i < bufferSize; i++) {
	                    double x = data.get(i) * vol;
	                    double y = (_a0 * x + _a1 * _x1 + _a2 * _x2 - _b1 * _y1 - _b2 * _y2);
	                    data.put( (float) (y * _gain) );
	                    _x2 = _x1;
	                    _x1 = x;
	                    _y2 = _y1;
	                    _y1 = y;
	                }
	            } else {
	                for (int i = 0; i < bufferSize; i++) {
	                    _a0 += a0_delta;
	                    _a1 += a1_delta;
	                    _a2 += a2_delta;
	                    _b1 += b1_delta;
	                    _b2 += b2_delta;
	                    _gain += gain_delta;
	                    double x = data.get(i) * vol;
	                    double y = (_a0 * x + _a1 * _x1 + _a2 * _x2 - _b1 * _y1 - _b2 * _y2);
	                    data.put( (float) (y * _gain) );
	                    _x2 = _x1;
	                    _x1 = x;
	                    _y2 = _y1;
	                    _y1 = y;
	                }
	            }
	
	            if (Math.abs(_x1) < 1.0E-8) {
	                _x1 = 0;
	            }
	            if (Math.abs(_x2) < 1.0E-8) {
	                _x2 = 0;
	            }
	            if (Math.abs(_y1) < 1.0E-8) {
	                _y1 = 0;
	            }
	            if (Math.abs(_y2) < 1.0E-8) {
	                _y2 = 0;
	            }
	            this.x1 = _x1;
	            this.x2 = _x2;
	            this.y1 = _y1;
	            this.y2 = _y2;
	        } 
	
	        this.last_a0 = this.a0;
	        this.last_a1 = this.a1;
	        this.last_a2 = this.a2;
	        this.last_b1 = this.b1;
	        this.last_b2 = this.b2;
	        this.last_q = this.q;
	        this.last_gain = this.gain;
	        this.last_wet = this.wet;
	
	    }
	
	    protected void filter2Add(float[] in, float[] out) {
	
	        if (dirty) {
	            filter2calc();
	            dirty = false;
	        }
	        if (!last_set) {
	            last_a0 = a0;
	            last_a1 = a1;
	            last_a2 = a2;
	            last_b1 = b1;
	            last_b2 = b2;
	            last_q = q;
	            last_gain = gain;
	            last_wet = wet;
	            last_set = true;
	        }
	
	        if (wet > 0 || last_wet > 0) {
	
	            double _a0 = this.last_a0;
	            double _a1 = this.last_a1;
	            double _a2 = this.last_a2;
	            double _b1 = this.last_b1;
	            double _b2 = this.last_b2;
	            double _gain = this.last_gain;
	            double _wet = this.last_wet;
	            double a0_delta = (this.a0 - this.last_a0) / bufferSize;
	            double a1_delta = (this.a1 - this.last_a1) / bufferSize;
	            double a2_delta = (this.a2 - this.last_a2) / bufferSize;
	            double b1_delta = (this.b1 - this.last_b1) / bufferSize;
	            double b2_delta = (this.b2 - this.last_b2) / bufferSize;
	            double gain_delta = (this.gain - this.last_gain) / bufferSize;
	            double wet_delta = (this.wet - this.last_wet) / bufferSize;
	            double _x1 = this.x1;
	            double _x2 = this.x2;
	            double _y1 = this.y1;
	            double _y2 = this.y2;
	
	            if (wet_delta != 0) {
	                for (int i = 0; i < bufferSize; i++) {
	                    _a0 += a0_delta;
	                    _a1 += a1_delta;
	                    _a2 += a2_delta;
	                    _b1 += b1_delta;
	                    _b2 += b2_delta;
	                    _gain += gain_delta;
	                    _wet += wet_delta;
	                    double x = in[i];
	                    double y = (_a0 * x + _a1 * _x1 + _a2 * _x2 - _b1 * _y1 - _b2 * _y2);
	                    out[i] += (y * _gain) * _wet + (x) * (1 - _wet);
	                    _x2 = _x1;
	                    _x1 = x;
	                    _y2 = _y1;
	                    _y1 = y;
	                }
	            } else if (a0_delta == 0 && a1_delta == 0 && a2_delta == 0 && b1_delta == 0 && b2_delta == 0) {
	                for (int i = 0; i < bufferSize; i++) {
	                    double x = in[i];
	                    double y = (_a0 * x + _a1 * _x1 + _a2 * _x2 - _b1 * _y1 - _b2 * _y2);
	                    out[i] += y * _gain;
	                    _x2 = _x1;
	                    _x1 = x;
	                    _y2 = _y1;
	                    _y1 = y;
	                }
	            } else {
	                for (int i = 0; i < bufferSize; i++) {
	                    _a0 += a0_delta;
	                    _a1 += a1_delta;
	                    _a2 += a2_delta;
	                    _b1 += b1_delta;
	                    _b2 += b2_delta;
	                    _gain += gain_delta;
	                    double x = in[i];
	                    double y = (_a0 * x + _a1 * _x1 + _a2 * _x2 - _b1 * _y1 - _b2 * _y2);
	                    out[i] += y * _gain;
	                    _x2 = _x1;
	                    _x1 = x;
	                    _y2 = _y1;
	                    _y1 = y;
	                }
	            }
	
	            if (Math.abs(_x1) < 1.0E-8) {
	                _x1 = 0;
	            }
	            if (Math.abs(_x2) < 1.0E-8) {
	                _x2 = 0;
	            }
	            if (Math.abs(_y1) < 1.0E-8) {
	                _y1 = 0;
	            }
	            if (Math.abs(_y2) < 1.0E-8) {
	                _y2 = 0;
	            }
	            this.x1 = _x1;
	            this.x2 = _x2;
	            this.y1 = _y1;
	            this.y2 = _y2;
	        } else {
	            for (int i = 0; i < bufferSize; i++) {
	                out[i] += in[i];
	            }
	        }
	
	        this.last_a0 = this.a0;
	        this.last_a1 = this.a1;
	        this.last_a2 = this.a2;
	        this.last_b1 = this.b1;
	        this.last_b2 = this.b2;
	        this.last_q = this.q;
	        this.last_gain = this.gain;
	        this.last_wet = this.wet;
	
	    }
	
	    protected void filter1calc() {
	        if (freqeuncy < 110) {
	            freqeuncy = 110;
	        }
	        double c = (7.0 / 6.0) * Math.PI * 2 * freqeuncy / sampleRate;
	        if (c > 1) {
	            c = 1;
	        }
	        a0 = (Math.sqrt(1 - Math.cos(c)) * Math.sqrt(0.5 * Math.PI));
	        if (resonancedB < 0) {
	            resonancedB = 0;
	        }
	        if (resonancedB > 20) {
	            resonancedB = 20;
	        }
	        q = (Math.sqrt(0.5) * Math.pow(10.0, -(resonancedB / 20)));
	        gain = Math.pow(10, -((resonancedB)) / 40.0);
	        if (wet == 0.0f) {
	            if (resonancedB > 0.00001 || c < 0.9999999) {
	                wet = 1.0f;
	            }
	        }
	    }
	
	    protected void filter1Add(float[] data, float vol) {
	
	        if (dirty) {
	            filter1calc();
	            dirty = false;
	        }
	        if (!last_set) {
	            last_a0 = a0;
	            last_q = q;
	            last_gain = gain;
	            last_wet = wet;
	            last_set = true;
	        }
	
	        if (wet > 0 || last_wet > 0) {
	
	            double _a0 = this.last_a0;
	            double _q = this.last_q;
	            double _gain = this.last_gain;
	            double _wet = this.last_wet;
	            double a0_delta = (this.a0 - this.last_a0) / bufferSize;
	            double q_delta = (this.q - this.last_q) / bufferSize;
	            double gain_delta = (this.gain - this.last_gain) / bufferSize;
	            double wet_delta = (this.wet - this.last_wet) / bufferSize;
	            double _y2 = this.y2;
	            double _y1 = this.y1;
	
	            if (wet_delta != 0) {
	            	if (vol == 1f)
		                for (int i = 0; i < bufferSize; i++) {
		                    _a0 += a0_delta;
		                    _q += q_delta;
		                    _gain += gain_delta;
		                    _wet += wet_delta;
		                    _y1 = (1 - _q * _a0) * _y1 - (_a0) * _y2 + (_a0) * data[i];
		                    _y2 = (1 - _q * _a0) * _y2 + (_a0) * _y1;
		                    data[i] = data[i] + (float) (_y2 * _gain * _wet + data[i] * (1 - _wet));
		                }
	            	else 
		                for (int i = 0; i < bufferSize; i++) {
		                    _a0 += a0_delta;
		                    _q += q_delta;
		                    _gain += gain_delta;
		                    _wet += wet_delta;
		                    _y1 = (1 - _q * _a0) * _y1 - (_a0) * _y2 + (_a0) * (data[i] * vol);
		                    _y2 = (1 - _q * _a0) * _y2 + (_a0) * _y1;
		                    data[i] = data[i] + (float) (_y2 * _gain * _wet + (data[i] * vol) * (1 - _wet));
		                }
	            } else if (a0_delta == 0 && q_delta == 0) {
	            	if (vol == 1f) 
		                for (int i = 0; i < bufferSize; i++) {
		                    _y1 = (1 - _q * _a0) * _y1 - (_a0) * _y2 + (_a0) * data[i];
		                    _y2 = (1 - _q * _a0) * _y2 + (_a0) * _y1;
		                    data[i] = data[i] + (float) (_y2 * _gain);
		                }
	            	else	            		
		                for (int i = 0; i < bufferSize; i++) {
		                    _y1 = (1 - _q * _a0) * _y1 - (_a0) * _y2 + (_a0) * (data[i] * vol);
		                    _y2 = (1 - _q * _a0) * _y2 + (_a0) * _y1;
		                    data[i] = data[i] + (float) (_y2 * _gain);
		                }
	            } else {
	            	if (vol == 1f)
		                for (int i = 0; i < bufferSize; i++) {
		                    _a0 += a0_delta;
		                    _q += q_delta;
		                    _gain += gain_delta;
		                    _y1 = (1 - _q * _a0) * _y1 - (_a0) * _y2 + (_a0) * data[i];
		                    _y2 = (1 - _q * _a0) * _y2 + (_a0) * _y1;
		                    data[i] = data[i] + (float) (_y2 * _gain);
		                }
	            	else
		                for (int i = 0; i < bufferSize; i++) {
		                    _a0 += a0_delta;
		                    _q += q_delta;
		                    _gain += gain_delta;
		                    _y1 = (1 - _q * _a0) * _y1 - (_a0) * _y2 + (_a0) * (data[i] * vol);
		                    _y2 = (1 - _q * _a0) * _y2 + (_a0) * _y1;
		                    data[i] = data[i] + (float) (_y2 * _gain);
		                }
	            }
	
	            if (Math.abs(_y2) < 1.0E-8) {
	                _y2 = 0;
	            }
	            if (Math.abs(_y1) < 1.0E-8) {
	                _y1 = 0;
	            }
	            this.y2 = _y2;
	            this.y1 = _y1;
	        }
	        
	        this.last_a0 = this.a0;
	        this.last_q = this.q;
	        this.last_gain = this.gain;
	        this.last_wet = this.wet;
	    }
	    
	    
	    protected void filter1Replace(FloatBuffer data, float vol) {
	
	        if (dirty) {
	            filter1calc();
	            dirty = false;
	        }
	        if (!last_set) {
	            last_a0 = a0;
	            last_q = q;
	            last_gain = gain;
	            last_wet = wet;
	            last_set = true;
	        }
	
	        if (wet > 0 || last_wet > 0) {
	
	            double _a0 = this.last_a0;
	            double _q = this.last_q;
	            double _gain = this.last_gain;
	            double _wet = this.last_wet;
	            double a0_delta = (this.a0 - this.last_a0) / bufferSize;
	            double q_delta = (this.q - this.last_q) / bufferSize;
	            double gain_delta = (this.gain - this.last_gain) / bufferSize;
	            double wet_delta = (this.wet - this.last_wet) / bufferSize;
	            double _y2 = this.y2;
	            double _y1 = this.y1;
	
	            if (wet_delta != 0) {
	            	if (vol == 1f)
		                for (int i = 0; i < bufferSize; i++) {
		                    _a0 += a0_delta;
		                    _q += q_delta;
		                    _gain += gain_delta;
		                    _wet += wet_delta;
		                    _y1 = (1 - _q * _a0) * _y1 - (_a0) * _y2 + (_a0) * data.get(i);
		                    _y2 = (1 - _q * _a0) * _y2 + (_a0) * _y1;
		                    data.put( (float) (_y2 * _gain * _wet + data.get(i) * (1 - _wet)) );
		                }
	            	else 
		                for (int i = 0; i < bufferSize; i++) {
		                    _a0 += a0_delta;
		                    _q += q_delta;
		                    _gain += gain_delta;
		                    _wet += wet_delta;
		                    _y1 = (1 - _q * _a0) * _y1 - (_a0) * _y2 + (_a0) * (data.get(i) * vol);
		                    _y2 = (1 - _q * _a0) * _y2 + (_a0) * _y1;
		                    data.put( (float) (_y2 * _gain * _wet + (data.get(i) * vol) * (1 - _wet)) );
		                }
	            } else if (a0_delta == 0 && q_delta == 0) {
	            	if (vol == 1f) 
		                for (int i = 0; i < bufferSize; i++) {
		                    _y1 = (1 - _q * _a0) * _y1 - (_a0) * _y2 + (_a0) * data.get(i);
		                    _y2 = (1 - _q * _a0) * _y2 + (_a0) * _y1;
		                    data.put( (float) (_y2 * _gain) );
		                }
	            	else	            		
		                for (int i = 0; i < bufferSize; i++) {
		                    _y1 = (1 - _q * _a0) * _y1 - (_a0) * _y2 + (_a0) * (data.get(i) * vol);
		                    _y2 = (1 - _q * _a0) * _y2 + (_a0) * _y1;
		                    data.put( (float) (_y2 * _gain) );
		                }
	            } else {
	            	if (vol == 1f)
		                for (int i = 0; i < bufferSize; i++) {
		                    _a0 += a0_delta;
		                    _q += q_delta;
		                    _gain += gain_delta;
		                    _y1 = (1 - _q * _a0) * _y1 - (_a0) * _y2 + (_a0) * data.get(i);
		                    _y2 = (1 - _q * _a0) * _y2 + (_a0) * _y1;
		                    data.put( (float) (_y2 * _gain) );
		                }
	            	else
		                for (int i = 0; i < bufferSize; i++) {
		                    _a0 += a0_delta;
		                    _q += q_delta;
		                    _gain += gain_delta;
		                    _y1 = (1 - _q * _a0) * _y1 - (_a0) * _y2 + (_a0) * (data.get(i) * vol);
		                    _y2 = (1 - _q * _a0) * _y2 + (_a0) * _y1;
		                    data.put( (float) (_y2 * _gain) );
		                }
	            }
	
	            if (Math.abs(_y2) < 1.0E-8) {
	                _y2 = 0;
	            }
	            if (Math.abs(_y1) < 1.0E-8) {
	                _y1 = 0;
	            }
	            this.y2 = _y2;
	            this.y1 = _y1;
	        }
	        
	        this.last_a0 = this.a0;
	        this.last_q = this.q;
	        this.last_gain = this.gain;
	        this.last_wet = this.wet;
	    }
	
	    protected void filter1Add(float[] in, float[] out) {
	
	        if (dirty) {
	            filter1calc();
	            dirty = false;
	        }
	        if (!last_set) {
	            last_a0 = a0;
	            last_q = q;
	            last_gain = gain;
	            last_wet = wet;
	            last_set = true;
	        }
	
	        if (wet > 0 || last_wet > 0) {
	
	            double _a0 = this.last_a0;
	            double _q = this.last_q;
	            double _gain = this.last_gain;
	            double _wet = this.last_wet;
	            double a0_delta = (this.a0 - this.last_a0) / bufferSize;
	            double q_delta = (this.q - this.last_q) / bufferSize;
	            double gain_delta = (this.gain - this.last_gain) / bufferSize;
	            double wet_delta = (this.wet - this.last_wet) / bufferSize;
	            double _y2 = this.y2;
	            double _y1 = this.y1;
	
	            if (wet_delta != 0) {
	                for (int i = 0; i < bufferSize; i++) {
	                    _a0 += a0_delta;
	                    _q += q_delta;
	                    _gain += gain_delta;
	                    _wet += wet_delta;
	                    _y1 = (1 - _q * _a0) * _y1 - (_a0) * _y2 + (_a0) * in[i];
	                    _y2 = (1 - _q * _a0) * _y2 + (_a0) * _y1;
	                    out[i] += _y2 * _gain * _wet + in[i] * (1 - _wet);
	                }
	            } else if (a0_delta == 0 && q_delta == 0) {
	                for (int i = 0; i < bufferSize; i++) {
	                    _y1 = (1 - _q * _a0) * _y1 - (_a0) * _y2 + (_a0) * in[i];
	                    _y2 = (1 - _q * _a0) * _y2 + (_a0) * _y1;
	                    out[i] += _y2 * _gain;
	                }
	            } else {
	                for (int i = 0; i < bufferSize; i++) {
	                    _a0 += a0_delta;
	                    _q += q_delta;
	                    _gain += gain_delta;
	                    _y1 = (1 - _q * _a0) * _y1 - (_a0) * _y2 + (_a0) * in[i];
	                    _y2 = (1 - _q * _a0) * _y2 + (_a0) * _y1;
	                    out[i] += _y2 * _gain;
	                }
	            }
	
	            if (Math.abs(_y2) < 1.0E-8) {
	                _y2 = 0;
	            }
	            if (Math.abs(_y1) < 1.0E-8) {
	                _y1 = 0;
	            }
	            this.y2 = _y2;
	            this.y1 = _y1;
	        } else {
	            for (int i = 0; i < bufferSize; i++) {
	                out[i] += in[i];
	            }
	        }
	
	        this.last_a0 = this.a0;
	        this.last_q = this.q;
	        this.last_gain = this.gain;
	        this.last_wet = this.wet;
	    }
	}

}
