package net.judah.fx;

//https://github.com/jaudiolibs/audioops/blob/master/audioops-impl/src/main/java/org/jaudiolibs/audioops/impl/IIRFilterOp.java
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
* @author Neil C Smith (derived from code by Karl Helgason)
*/
import java.nio.FloatBuffer;
import java.security.InvalidParameterException;

import lombok.Getter;
import net.judah.api.Effect;
import net.judah.util.Constants;

/**
 * IIR Filter op ported from Gervill in OpenJDK.
 * This is a MONO op and requires one input and output channel.
 * Includes Resonance, Wet/Dry, Smoothing
 */
public class MonoFilter implements Effect {

    public enum Settings { Type, Frequency, Resonance }
    public static enum Type { HiCut, LoCut }

    private final int BUF_SIZE; // anti-alias upsample-able
    private final int SR; // anti-alias upsample-able

    /** y = 1/30 * x ^ 2.81 + bassFloor */
	public static float knobToFrequency(int val) {
		return (float)(0.033333 * Math.pow((val + 1), 2.81)) + 50f;
	}

	static float[] reverse = new float[100];
	static {
	    for (int i = 0; i < reverse.length; i++)
	        reverse[i] = knobToFrequency(i);
	}
	public static int frequencyToKnob(float freq) {
	    for (int i = 0; i < reverse.length; i++)
	        if (reverse[i] >= freq)
	            return i;
	    return 100;
	}

	@Getter private final String name = MonoFilter.class.getSimpleName();
	@Getter int paramCount = Settings.values().length;
    @Getter private Type filterType;
    @Getter private float frequency = 666;
    private double resonancedB = 1;
    private final IIRFilter filter = new IIRFilter();

    public MonoFilter(Type type, float freq, int upsample) {
    	BUF_SIZE = upsample * Constants.bufSize();
    	SR = upsample * Constants.sampleRate();
    	filterType = type;
    	setFrequency(freq);
    	filter.reset();
    }

    @Override public int get(int idx) {
        if (idx == Settings.Type.ordinal())
            return getFilterType().ordinal();
        if (idx == Settings.Frequency.ordinal())
            return frequencyToKnob(getFrequency());
        if (idx == Settings.Resonance.ordinal())
            return (int)(resonancedB * 4);
        throw new InvalidParameterException();
    }

    @Override public void set(int idx, int value) {
        if (idx == Settings.Type.ordinal())
            setFilterType(Type.values()[value]);
        else if (idx == Settings.Frequency.ordinal())
            setFrequency(knobToFrequency(value));
        else if (idx == Settings.Resonance.ordinal())
        	setResonance(value * 0.25f);
        else throw new InvalidParameterException();
    }

    /**Set frequency of filter in Hz. <br/>
     *  Recommended range (20 - samplerate/2) */
    public void setFrequency(float hz) {
        if (frequency == hz)
            return;
        frequency = hz;
        filter.dirty = true;
    }

    /**Set resonance of filter in dB. <br/>
     * Recommended range (0 - 30)*/
    public void setResonance(float db) {
        if (resonancedB == db)
            return;
        resonancedB = db;
        filter.dirty = true;
    }

    /** @return resonance of filter. */
    public float getResonance() {
        return (float) resonancedB;
    }

    public void setFilterType(Type filtertype) {
    	if (this.filterType == filtertype) return;
        this.filterType = filtertype;
        filter.dirty = true;
    }


    /** process replace mono */
    public void process(FloatBuffer mono) {
    	filter.filter2Replace(mono);
    }

    /** left-channel only */
    @Override public void process(FloatBuffer left, FloatBuffer right) {
    	process(left);
    }

    class IIRFilter {

        private boolean dirty = true;
	    private double x1;
	    private double x2;
	    private double y1;
	    private double y2;
	    private double a0;
	    private double a1;
	    private double a2;
	    private double b1;
	    private double b2;
	    private double gain = 1;
	    private double wet = 0;
	    private double last_wet = 0;
	    private double last_a0;
	    private double last_a1;
	    private double last_a2;
	    private double last_b1;
	    private double last_b2;
	    private double last_gain;
	    private boolean last_set = false;

		void reset() {
	        dirty = true;
	        last_set = false;
	        wet = 0.0f;
	        gain = 1.0f;

	        x1 = 0; x2 = 0;
	        y1 = 0; y2 = 0;
	        a0 = 0; a1 = 0;
	        a2 = 0;
	        b1 = 0; b2 = 0;
	    }

	    private void filter2calc() {

	        double rdB = resonancedB;
	        if (rdB < 0) {
	            rdB = 0;    // Negative dB are illegal.
	        }
	        if (rdB > 30) {
	            rdB = 30;   // At least 22.5 dB is needed.
	        }

			if (filterType == Type.HiCut) {
	            double r = (frequency / SR);
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

			if (filterType == Type.LoCut) {
	            double r = (frequency / SR);
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

	    private void filter2Replace(FloatBuffer data) {
	    	data.rewind();

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
	            double a0_delta = (this.a0 - this.last_a0) / BUF_SIZE;
	            double a1_delta = (this.a1 - this.last_a1) / BUF_SIZE;
	            double a2_delta = (this.a2 - this.last_a2) / BUF_SIZE;
	            double b1_delta = (this.b1 - this.last_b1) / BUF_SIZE;
	            double b2_delta = (this.b2 - this.last_b2) / BUF_SIZE;
	            double gain_delta = (this.gain - this.last_gain) / BUF_SIZE;
	            double wet_delta = (this.wet - this.last_wet) / BUF_SIZE;
	            double _x1 = this.x1;
	            double _x2 = this.x2;
	            double _y1 = this.y1;
	            double _y2 = this.y2;

	            if (wet_delta != 0) {
	                for (int i = 0; i < BUF_SIZE; i++) {
	                    _a0 += a0_delta;
	                    _a1 += a1_delta;
	                    _a2 += a2_delta;
	                    _b1 += b1_delta;
	                    _b2 += b2_delta;
	                    _gain += gain_delta;
	                    _wet += wet_delta;
	                    double x = data.get(i);
	                    double y = (_a0 * x + _a1 * _x1 + _a2 * _x2 - _b1 * _y1 - _b2 * _y2);
	                    data.put( (float) ((y * _gain) * _wet + (x) * (1 - _wet)) );
	                    _x2 = _x1;
	                    _x1 = x;
	                    _y2 = _y1;
	                    _y1 = y;
	                }
	            } else if (a0_delta == 0 && a1_delta == 0 && a2_delta == 0
	            		&& b1_delta == 0 && b2_delta == 0) {
	                for (int i = 0; i < BUF_SIZE; i++) {
	                    double x = data.get(i);
	                    double y = (_a0 * x + _a1 * _x1 + _a2 * _x2 - _b1 * _y1 - _b2 * _y2);
	                    data.put( (float) (y * _gain) );
	                    _x2 = _x1;
	                    _x1 = x;
	                    _y2 = _y1;
	                    _y1 = y;
	                }
	            } else {
	                for (int i = 0; i < BUF_SIZE; i++) {
	                    _a0 += a0_delta;
	                    _a1 += a1_delta;
	                    _a2 += a2_delta;
	                    _b1 += b1_delta;
	                    _b2 += b2_delta;
	                    _gain += gain_delta;
	                    double x = data.get(i);
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
	        this.last_gain = this.gain;
	        this.last_wet = this.wet;

	    }

	}

}
