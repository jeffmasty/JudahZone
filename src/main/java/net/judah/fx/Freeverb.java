package net.judah.fx;

/* Derived from code in JAudioLibs http://neilcsmith.net
 * https://github.com/jaudiolibs/audioops/blob/master/audioops-impl/src/main/java/org/jaudiolibs/audioops/impl/FreeverbOp.java
 * Copyright 2010 Neil C Smith.
 *  GNU2 License
 *
 * Derived from code in RasmsuDSP
 * Copyright (c) 2006, Karl Helgason
 * All rights reserved.
 */

import java.nio.FloatBuffer;
import java.security.InvalidParameterException;
import java.util.Arrays;

import lombok.Getter;
import lombok.Setter;

/** The classic Freeverb algorithm, true stereo with independent L/R filter networks */
public final class Freeverb extends Reverb {

    private static final float fixedgain = 0.01f;
    private static final float scalewet = 1;
    private static final float scaledry = 2;
    private static final float scaledamp = 0.4f;
    private static final float scaleroom = 0.49f;
    private static final float offsetroom = 0.5f;

    private static final float initialroom = 0.6f;
    private static final float initialdamp = 1.5f;
    private static final float initialwet = 0.4f;
    private static final float initialdry = 0.5f;//1;
    private static final float initialwidth = 0.9f;

    @Getter private final int paramCount = Settings.values().length;
//    @Getter private boolean active;

    private float roomsize;
    private float damp;
    private float wet;
    private float dry;
    private float width;
    private boolean dirty;
    // Comb filters
    private int numcombs;
    private Comb[] combL;
    private Comb[] combR;
    // Allpass filters
    private int numallpasses;
    private Allpass[] allpassL;
    private Allpass[] allpassR;
    //scratch buffers
    private float[] inScratchL = new float[N_FRAMES];
    private float[] inScratchR = new float[N_FRAMES];
    private float[] outScratchL = new float[N_FRAMES];
    private float[] outScratchR = new float[N_FRAMES];

    public Freeverb() {
        setWet(initialwet);
        setRoomSize(initialroom);
        setDry(initialdry);
        setDamp(initialdamp);
        setWidth(initialwidth);

        int[] delays = { 1111, 1203, 1273, 1373, 1424, 1477, 1548, 1593, 1659, 1694, 1727, 1760 };
        numcombs = delays.length;
        combL = new Comb[numcombs];
        combR = new Comb[numcombs];
        // create R delays slightly offset to decorrelate channels
        final int rightOffset = 23;
        for (int i = 0; i < numcombs; i++) {
            combL[i] = new Comb(delays[i]);
            combR[i] = new Comb(delays[i] + rightOffset);
        }

        int[] tuning = {408,  616,   550, 467, 321, 239};
        numallpasses = tuning.length;
        allpassL = new Allpass[numallpasses];
        allpassR = new Allpass[numallpasses];
        for (int i = 0; i < numallpasses; i++) {
            int sizeL = tuning[i];
            int sizeR = Math.max(1, sizeL + (i % 2 == 0 ? 11 : -7)); // small allpassR decorrelation
            allpassL[i] = new Allpass(sizeL);
            allpassR[i] = new Allpass(sizeR);
        }
        for (int i = 0; i < numallpasses; i++) {
            allpassL[i].setFeedback(0.6f);
            allpassR[i].setFeedback(0.6f);
        }

        // prepare all buffers!
        dirty = true;
    }

    @Override
    public int get(int idx) {
        if (idx == Settings.Room.ordinal())
            return Math.round(getRoomSize() * 100);
        if (idx == Settings.Damp.ordinal())
            return Math.round(getDamp() * 50);
        if (idx == Settings.Wet.ordinal())
            return Math.round(getWet() * 100);
        if (idx == Settings.Width.ordinal())
        	return Math.round(width * 100);
        throw new InvalidParameterException();
    }

    @Override
    public void set(int idx, int value) {
        if (idx == Settings.Room.ordinal())
            setRoomSize(value * 0.01f);
        else if (idx == Settings.Damp.ordinal())
            setDamp(value * 0.02f);
        else if (idx == Settings.Wet.ordinal())
            setWet(value * 0.01f);
        else if (idx == Settings.Width.ordinal())
        	setWidth(value * 0.01f);
        else throw new InvalidParameterException();
    }

    @Override public boolean isInternal() { return true; }

    @Override
    public void setRoomSize(float value) {
        roomsize = (value * scaleroom) + offsetroom;
        dirty = true;
    }

    @Override
    public float getRoomSize() {
        return (roomsize - offsetroom) / scaleroom;
    }

    @Override
    public void setDamp(float value) {
        damp = value * scaledamp;
        dirty = true;
    }

    @Override
    public float getDamp() {
        return damp / scaledamp;
    }

    @Override
    public void setWet(float value) {
        wet = value * scalewet;
        dirty = true;
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
	public void activate() {
    	dirty = true;
    }

    private void update() {
        for (int i = 0; i < numcombs; i++) {
            combL[i].setFeedback(roomsize);
            combL[i].setdamp(damp);
            combR[i].setFeedback(roomsize);
            combR[i].setdamp(damp);
        }
        dirty = false;
    }

    @Override
    public void process(FloatBuffer left, FloatBuffer right) {
        if (right == null) {
            // mono: process only left, produce mono wet (no cross-channel)
            processMono(left);
            return;
        }

        // stereo: process both buffers together
        left.rewind();
        right.rewind();
        if (dirty) update();

        float ourGain = fixedgain;
        for (int i = 0; i < N_FRAMES; i++) {
            inScratchL[i] = left.get(i) * ourGain;
            inScratchR[i] = right.get(i) * ourGain;
        }

        Arrays.fill(outScratchL, 0);
        Arrays.fill(outScratchR, 0);

        // ---- Run the first couple of allpass filters as pre-diffusion ----
        final int preAllpasses = Math.min(2, numallpasses);
        for (int i = 0; i < preAllpasses; i++) {
            allpassL[i].processReplace(inScratchL, inScratchL);
            allpassR[i].processReplace(inScratchR, inScratchR);
        }

        // Run combs on the diffused input
        for (int i = 0; i < numcombs; i++) {
            combL[i].processMix(inScratchL, outScratchL);
            combR[i].processMix(inScratchR, outScratchR);
        }

        // ---- Run any remaining allpasses as post-diffusion (as before) ----
        for (int i = preAllpasses; i < numallpasses; i++) {
            allpassL[i].processReplace(outScratchL, outScratchL);
            allpassR[i].processReplace(outScratchR, outScratchR);
        }

        // compute width mixes
        // wet1 = wet * (width/2 + 0.5)  -> primary (same-channel) wet gain
        // wet2 = wet * ((1 - width)/2) -> cross-channel wet gain
        float wet1 = wet * (width / 2.0f + 0.5f);
        float wet2 = wet * ((1.0f - width) / 2.0f);

        // write back outputs with wet/dry + stereo width cross-mix
        for (int i = 0; i < N_FRAMES; i++) {
            float inL = left.get(i);
            float inR = right.get(i);
            float reverbL = outScratchL[i];
            float reverbR = outScratchR[i];

            float outL = inL + reverbL * wet1 + reverbR * wet2;
            float outR = inR + reverbR * wet1 + reverbL * wet2;

            left.put(i, outL);
            right.put(i, outR);
        }
    }

    private void processMono(FloatBuffer buf) {
        buf.rewind();
        if (dirty) update();

        float ourGain = fixedgain;
        for (int i = 0; i < N_FRAMES; i++)
            inScratchL[i] = buf.get(i) * ourGain;

        float[] work = outScratchL;
        Arrays.fill(work, 0);

        final int preAllpasses = Math.min(2, numallpasses);
        for (int i = 0; i < preAllpasses; i++)
            allpassL[i].processReplace(inScratchL, inScratchL);

        for (int i = 0; i < numcombs; i++)
            combL[i].processMix(inScratchL, work);

        for (int i = preAllpasses; i < numallpasses; i++)
            allpassL[i].processReplace(work, work);

        for (int i = 0; i < N_FRAMES; i++)
            buf.put(i, buf.get(i) + work[i] * wet); // simple mono wet
    }

    private class Comb {

        @Setter float feedback; // roomsize
        float filterstore = 0;
        float damp1;
        float damp2;
        float[] buffer;
        final int bufsize;
        int bufidx = 0;

        public Comb(int size) {
            bufsize = Math.max(1, size);
            reset();
        }

        public void reset() {
            buffer = new float[bufsize];
            bufidx = 0;
            filterstore = 0;
        }

        public void processMix(float inputs[], float outputs[]) {
            for (int i = 0; i < N_FRAMES; i++) {
                float output = buffer[bufidx];

                // undenormalise
                if (output > 0.0f && output < 1.0E-9f)
                    output = 0;
                if (output < 0.0f && output > -1.0E-9f)
                    output = 0;

                filterstore = (output * damp2) + (filterstore * damp1);
                // undenormalise(filterstore);
                if (filterstore > 0.0f && filterstore < 1.0E-9f)
                    filterstore = 0;
                else if (filterstore < 0.0f && filterstore > -1.0E-9f)
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

        @Setter float feedback = 0.5f;
        float[] buffer;
        int bufidx = 0;
        final int size;

        public Allpass(int size) {
            this.size = Math.max(1, size);
            reset();
        }

        public void reset() {
            buffer = new float[size];
            bufidx = 0;
        }

        public void processReplace(float inputs[], float outputs[]) {
            float input;
            for (int i = 0; i < N_FRAMES; i++) {

                // undenormalise
                if (buffer[bufidx] > 0 && buffer[bufidx] < 1.0E-9)
                    buffer[bufidx] = 0;
                else if (buffer[bufidx] < 0.0 && buffer[bufidx] > -1.0E-9)
                    buffer[bufidx] = 0;

                input = inputs[i];
                outputs[i] = -input + buffer[bufidx];
                buffer[bufidx] = input + buffer[bufidx] * feedback;
                if (++bufidx >= size) {
                    bufidx = 0;
                }
            }
        }
   }

    @Override
    public void reset() {
        for (Allpass l : allpassL)
            l.reset();
        for (Comb l : combL)
            l.reset();
        for (Allpass r : allpassR)
            r.reset();
        for (Comb r : combR)
            r.reset();
    }

//    @Override
//    public void setActive(boolean active) {
//        this.active = active;
//        if (active) return;
//        Threads.execute(() -> { // clear the echo
//            for (Allpass l : allpassL)
//                l.reset();
//            for (Comb l : combL)
//                l.reset();
//            for (Allpass r : allpassR)
//                r.reset();
//            for (Comb r : combR)
//                r.reset();
//        });
//    }

}