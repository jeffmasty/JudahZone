package net.judah.fx;
/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2012 Neil C Smith.
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
 */
import java.nio.FloatBuffer;
import java.security.InvalidParameterException;

import judahzone.api.TimeEffect;
import lombok.Getter;
import lombok.Setter;

// https://github.com/jaudiolibs/audioops/blob/master/audioops-impl/src/main/java/org/jaudiolibs/audioops/impl/LFODelayOp.java
public class Chorus implements TimeEffect, judahzone.api.Effect.RTEffect {

    public enum Settings {
        Rate, Depth, Feedback, Type, Sync, Phase
    }

    private static final float PI2 = (float) Math.PI * 2;
    private static final float defaultRate = 1.4f;
    private static final float defaultDepth = 0.4f;
    private static final float defaultFeedback = 0.4f;

//    @Getter @Setter boolean active;
	@Setter @Getter boolean sync;
	@Setter @Getter String type = TYPE[0];

	/** times per second */
    @Getter private float rate = defaultRate;
    /** between 0 and 1 */
    @Getter private float depth = defaultDepth;
    /** between 0 and 1 */
    @Getter private float feedback = defaultFeedback;
    /** between 0 and 1 */
    @Getter @Setter private float phase = 0.42f;

    private final LFODelay leftDsp = new LFODelay();
    private final LFODelay rightDsp = new LFODelay();

	@Override
	public void sync(float unit) {
		int reverseIndex = TimeEffect.TYPE.length - TimeEffect.indexOf(type);
		rate = 0.001f * (unit + unit * reverseIndex);
	}

    @Override public String getName() {
        return Chorus.class.getSimpleName();
    }

    @Override
    public int getParamCount() {
        return Settings.values().length;
    }

    @Override
    public int get(int idx) {
        if (idx == Settings.Rate.ordinal())
            return Math.round(rate * 20);
        if (idx == Settings.Depth.ordinal())
            return Math.round(getDepth() * 100);
        if (idx == Settings.Feedback.ordinal())
            return Math.round(getFeedback() * 100);
        if (idx == Settings.Type.ordinal())
        	return TimeEffect.indexOf(type);
        if (idx == Settings.Sync.ordinal())
        	return sync ? 1 : 0;
        if (idx == Settings.Phase.ordinal())
        	return Math.round(phase * 100);
        throw new InvalidParameterException();
    }

    @Override
    public void set(int idx, int value) {
        if (idx == Settings.Rate.ordinal())
        	rate = value/20f;
        else if (idx == Settings.Depth.ordinal())
            setDepth(value/100f);
        else if (idx == Settings.Feedback.ordinal())
            feedback = value/100f;
        else if (idx == Settings.Type.ordinal() && value < TimeEffect.TYPE.length)
        	type = TimeEffect.TYPE[value];
        else if (idx == Settings.Sync.ordinal())
        	sync = value > 0;
        else if (idx == Settings.Phase.ordinal()) {
        	phase = value/100f;
        }
        else throw new InvalidParameterException();

    }

    void setDepth(float depth) {
        leftDsp.setDelay(depth / 1000);
        rightDsp.setDelay(depth / 1000);
        this.depth = depth;
    }

    @Override
	public void process(FloatBuffer left, FloatBuffer right) {
        leftDsp.processReplace(left);
        rightDsp.processReplace(right);
    }

    private class LFODelay {
        private int lfocount;
        @Setter float delay = depth * 0.001f;
        float[] workArea = new float[N_FRAMES];
        float range = 0.5f;
        float delayTime;
        int rovepos;
        float lastdelay;

        // 1-pole lowpass state and coefficient for the feedback path.
        // This is the only change from the original implementation: it tames HF buildup
        // in the feedback loop and reduces metallic ringing.
        float fbFilterState = 0f;
        final float fbCut = 0.25f; // 0..1, smaller -> stronger lowpass (tune to taste)

        void goFigure() {
            if (rate > 0.01 && range > 0) {
                lfocount += N_FRAMES;
                float lfolength = SAMPLE_RATE / rate;
                lfocount %= (int) (lfolength);
                float r = lfocount / lfolength;
                r *= PI2;
                r += phase * PI2;
                if (r > PI2) {
                    r -= PI2;
                }
                r = delay * range * (float) Math.sin(r);
                delayTime = delay + r;
            } else {
                lfocount = 0;
                delayTime = delay;
            }
        }

        void processReplace(FloatBuffer buf) {

            goFigure();
            buf.rewind();
            float delay = delayTime * SAMPLE_RATE;
            float ldelay = lastdelay;

            float[] work = workArea;
            int rnlen = work.length;
            int pos = rovepos;
            float delta = (delay - ldelay) / N_FRAMES;
            float fb = feedback;

            float r, s, a, b, o;
            int ri;
            for (int i = 0; i < N_FRAMES; i++) {
                r = pos - (ldelay + 2) + rnlen;
                ri = (int) r;
                s = r - ri;
                a = work[ri % rnlen];
                b = work[(ri + 1) % rnlen];
                o = a * (1 - s) + b * s;

                // apply 1-pole lowpass to the feedback sample before writing back into buffer
                float in = buf.get(i);
                float fbSample = o * fb;
                fbFilterState = fbFilterState + fbCut * (fbSample - fbFilterState);

                work[pos] = in + fbFilterState;
                buf.put(o);
                pos = (pos + 1) % rnlen;
                ldelay += delta;
            }

            rovepos = pos;
            lastdelay = delay;
        }

        @SuppressWarnings("unused")
		public void processAdd(FloatBuffer buf) {

            goFigure();
            buf.rewind();
            float delay = delayTime * SAMPLE_RATE;
            float ldelay = lastdelay;

            int rnlen = workArea.length;
            int pos = rovepos;
            float delta = (delay - ldelay) / N_FRAMES;

            float r, s, a, b, o;
            int ri;
                for (int i = 0; i < N_FRAMES; i++) {
                    r = pos - (ldelay + 2) + rnlen;
                    ri = (int) r;
                    s = r - ri;
                    a = workArea[ri % rnlen];
                    b = workArea[(ri + 1) % rnlen];
                    o = a * (1 - s) + b * s;

                    // apply 1-pole lowpass to feedback sample here as well
                    workArea[pos] = buf.get(i) + o * feedback;
                    float fbSample = o * feedback;
                    fbFilterState = fbFilterState + fbCut * (fbSample - fbFilterState);
                    // overwrite with filtered feedback contribution
                    workArea[pos] = buf.get(i) + fbFilterState;

                    buf.put(buf.get(i) + o);
                    pos = (pos + 1) % rnlen;
                    ldelay += delta;
                }
            rovepos = pos;
            lastdelay = delay;
        }
    }

}