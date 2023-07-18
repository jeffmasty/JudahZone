package net.judah.fx;

/*https://github.com/jaudiolibs/audioops/blob/master/audioops-impl/src/main/java/org/jaudiolibs/audioops/impl/FreeverbOp.java*/
/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2010 Neil C Smith.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details.
 *
 * You should have received a copy of the GNU General Public License version 2
 * along with this work; if not, see http://www.gnu.org/licenses/
 *
 *
 * Linking this work statically or dynamically with other modules is making a
 * combined work based on this work. Thus, the terms and conditions of the GNU
 * General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this work give you permission
 * to link this work with independent modules to produce an executable,
 * regardless of the license terms of these independent modules, and to copy and
 * distribute the resulting executable under terms of your choice, provided that
 * you also meet, for each linked independent module, the terms and conditions of
 * the license of that module. An independent module is a module which is not
 * derived from or based on this work. If you modify this work, you may extend
 * this exception to your version of the work, but you are not obligated to do so.
 * If you do not wish to do so, delete this exception statement from your version.
 *
 * Please visit http://neilcsmith.net if you need additional information or
 * have any questions.
 *
 *
 * Derived from code in RasmsuDSP
 * Copyright (c) 2006, Karl Helgason
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *    1. Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *    2. Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *    3. The name of the author may not be used to endorse or promote
 *       products derived from this software without specific prior
 *       written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import java.nio.FloatBuffer;
import java.security.InvalidParameterException;
import java.util.Arrays;

import lombok.Getter;
import lombok.Setter;
import net.judah.util.Constants;

/** the classic Freeverb algorithm, stereo handled as a separate internal Effect unit */
public final class Freeverb extends Reverb {

    private static final float fixedgain = 0.015f * 2;
    private static final float scalewet = 4;
    private static final float scaledry = 2;
    private static final float scaledamp = 0.4f;
    private static final float scaleroom = 0.28f;
    private static final float offsetroom = 0.7f;

    private static final float initialroom = 0.4f;
    private static final float initialdamp = 1f;
    private static final float initialwet = 1 / scalewet;
    private static final float initialdry = 0.5f;//1;
    private static final float initialwidth = 1.0f;

    @Getter private boolean active;
    private int nframes = Constants.bufSize();
    private float roomsize;
    private float damp;
    private float wet, wet1;
    private float dry;
    private float width;
    private boolean dirty;
    // Comb filters
    private int numcombs;
    private Comb[] combL;
    // Allpass filters
    private int numallpasses;
    private Allpass[] allpassL;
    //scratch buffers
    private float[] inScratch = null;
    private float[] outScratchL = null;
    
    private final Freeverb stereoReverb;

    public Freeverb(boolean isStereo) {
        initialize(Constants.sampleRate(), Constants.bufSize());
        stereoReverb = isStereo ? new Freeverb(false) : null;
    }

    @Override
    public void initialize(int samplerate, int maxBufferSize) {
		nframes = maxBufferSize;
        setWet(initialwet);
        setRoomSize(initialroom);
        setDry(initialdry);
        setDamp(initialdamp);
        setWidth(initialwidth);

        float freqscale = samplerate / 44100.0f;

        /* Init Comb filters */
        int combtuningL1 = (int) (freqscale * (1116));
        int combtuningL2 = (int) (freqscale * (1188));
        int combtuningL3 = (int) (freqscale * (1277));
        int combtuningL4 = (int) (freqscale * (1356));
        int combtuningL5 = (int) (freqscale * (1422));
        int combtuningL6 = (int) (freqscale * (1491));
        int combtuningL7 = (int) (freqscale * (1557));
        int combtuningL8 = (int) (freqscale * (1617));

        numcombs = 8;
        combL = new Comb[numcombs];
        combL[0] = new Comb(combtuningL1);
        combL[1] = new Comb(combtuningL2);

        combL[2] = new Comb(combtuningL3);
        combL[3] = new Comb(combtuningL4);
        combL[4] = new Comb(combtuningL5);
        combL[5] = new Comb(combtuningL6);
        combL[6] = new Comb(combtuningL7);
        combL[7] = new Comb(combtuningL8);

        /* Init Allpass filters*/
        int allpasstuningL1 = (int) (freqscale * (556));
        int allpasstuningL2 = (int) (freqscale * (441));
        int allpasstuningL3 = (int) (freqscale * (341));
        int allpasstuningL4 = (int) (freqscale * (225));

        numallpasses = 4;
        allpassL = new Allpass[numallpasses];
        allpassL[0] = new Allpass(allpasstuningL1);
        allpassL[1] = new Allpass(allpasstuningL2);
        allpassL[2] = new Allpass(allpasstuningL3);
        allpassL[3] = new Allpass(allpasstuningL4);

        for (int i = 0; i < numallpasses; i++) {
            allpassL[i].setFeedback(0.5f);
        }

        /* Init scratch buffers*/
        inScratch = new float[maxBufferSize];
        outScratchL = new float[maxBufferSize];

        /* Prepare all buffers*/
        update();
    }

    @Override
    public int getParamCount() {
        return Settings.values().length;
    }

    @Override
    public int get(int idx) {
        if (idx == Settings.Room.ordinal())
            return Math.round(getRoomSize() * 100);
        if (idx == Settings.Damp.ordinal())
            return Math.round(getDamp() * 100);
        if (idx == Settings.Wet.ordinal())
            return Math.round(getWet() * 100);
        throw new InvalidParameterException();
    }

    @Override
    public void set(int idx, int value) {
        if (idx == Settings.Room.ordinal())
            setRoomSize(value * 0.01f);
        else if (idx == Settings.Damp.ordinal())
            setDamp(value * 0.01f);
        else if (idx == Settings.Wet.ordinal())
            setWet(value * 0.01f);
        else throw new InvalidParameterException();
    }

    @Override public boolean isInternal() { return true; }

	@Override
    public void setRoomSize(float value) {
        roomsize = (value * scaleroom) + offsetroom;
        dirty = true;
        if (stereoReverb != null)
        	stereoReverb.setRoomSize(value);
    }

    @Override
    public float getRoomSize() {
        return (roomsize - offsetroom) / scaleroom;
    }

    @Override
    public void setDamp(float value) {
        damp = value * scaledamp;
        dirty = true;
        if (stereoReverb != null)
        	stereoReverb.setDamp(value);
    }

    @Override
    public float getDamp() {
        return damp / scaledamp;
    }

    @Override
    public void setWet(float value) {
        wet = value * scalewet;
        dirty = true;
        if (stereoReverb != null)
        	stereoReverb.setWet(value);
    }

    @Override
    public float getWet() {
        return wet / scalewet;
    }

    public void setDry(float value) {
        dry = value * scaledry;
        dirty = true;
    }

    public float getDry() {
        return dry / scaledry;
    }

    @Override
    public void setWidth(float value) {
        width = value;
        dirty = true;
    }

    @Override
    public float getWidth() {
        return width;
    }

    @Override
	public void process(FloatBuffer left, FloatBuffer right) {
    	process(left);
    	stereoReverb.process(right);
    }
    
    @Override
    public void process(FloatBuffer buf) {
    	buf.rewind();
        if (dirty) {
            update();
            dirty = false;
        }
        float ourGain = fixedgain;
        for (int i = 0; i < nframes; i++)
            inScratch[i] = buf.get(i) * ourGain;

        Arrays.fill(outScratchL, 0);

        for (int i = 0; i < numcombs; i++)
            combL[i].processMix(inScratch, outScratchL, nframes);

        for (int i = 0; i < numallpasses; i++)
            allpassL[i].processReplace(outScratchL, outScratchL, nframes);

            for (int i = 0; i < nframes; i++)
                buf.put(buf.get(i) + // process add
                		outScratchL[i] * wet1);// + outScratchR[i] * wet2);

    }

    private void update() {
        wet1 = wet * (width / 2 + 0.5f);
        for (int i = 0; i < numcombs; i++) {
            combL[i].setFeedback(roomsize);
            combL[i].setdamp(damp);
        }
        if (stereoReverb != null)
        	stereoReverb.update();
    }

    private class Comb {

        @Setter float feedback; // roomsize
        float filterstore = 0;
        float damp1;
        float damp2;
        float[] buffer;
        int bufsize;
        int bufidx = 0;

        public Comb(int size) {
            bufsize = size;
            reset();
        }
        
        public void reset() {
        	buffer = new float[bufsize];
        }
        

        public void processMix(float inputs[], float outputs[], int buffersize) {
            for (int i = 0; i < buffersize; i++) {
                float output = buffer[bufidx];

                //undenormalise(output);
                if (output > 0.0) {
                    if (output < 1.0E-9) {
                        output = 0;
                    }
                }
                if (output < 0.0) {
                    if (output > -1.0E-9) {
                        output = 0;
                    }
                }

                filterstore = (output * damp2) + (filterstore * damp1);
                //undenormalise(filterstore);
                if (filterstore > 0.0 && filterstore < 1.0E-9) 
                	filterstore = 0;
                else if (filterstore < 0.0 && filterstore > -1.0E-9) 
                        filterstore = 0;

                buffer[bufidx] = inputs[i] + (filterstore * feedback);

                if (++bufidx >= bufsize) 
                    bufidx = 0;
                

                outputs[i] += output;

            }
        }

        public void setdamp(float val) {
            damp1 = val;
            damp2 = 1 - val;
        }

    }

    private class Allpass {

        @Setter float feedback;
        float[] buffer;
        int bufsize;
        int bufidx = 0;

        public Allpass(int size) {
            bufsize = size;
        	reset();
        }

        public void reset() {
        	buffer = new float[bufsize];
        }
        
        public void processReplace(float inputs[], float outputs[], int buffersize) {
        	float input;
            for (int i = 0; i < buffersize; i++) {

                //undenormalise
            	if (buffer[bufidx] > 0 && buffer[bufidx] < 1.0E-9) 
            		buffer[bufidx] = 0;
            	else if (buffer[bufidx] < 0.0 && buffer[bufidx] > -1.0E-9) 
                	buffer[bufidx] = 0;

                input = inputs[i];
                outputs[i] = -input + buffer[bufidx];
                buffer[bufidx] = input + buffer[bufidx] * feedback;
                if (++bufidx >= bufsize) {
                    bufidx = 0;
                }
            }
        }
   }

	@Override
	public void setActive(boolean active) {
		this.active = active;
		if (stereoReverb != null)
			stereoReverb.setActive(active);
		if (active) return;
		Constants.execute(() -> { // clear the echo
			for (Allpass l : allpassL)
				l.reset();
			for (Comb l : combL)
				l.reset();
		});
	}


}