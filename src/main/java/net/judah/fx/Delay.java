package net.judah.fx;

import java.nio.FloatBuffer;
import java.security.InvalidParameterException;

/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2015 Neil C Smith.
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
 * to link this work with independent modules to produce a executable,
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
 *  *
 *
 * Derived from code in Gervill / OpenJDK
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
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

import java.util.Arrays;

import lombok.Getter;
import lombok.Setter;
import net.judah.api.TimeEffect;
import net.judah.util.Constants;

/**Two Identical Mono Delays.
 * Delay time and feedback are interpolated over the period of one buffer.
 * Some smoothing applied.
 * @author Neil C Smith (derived from code by Karl Helgason)
 * @author Jeff Masty
 */
public class Delay implements TimeEffect, net.judah.api.Effect.RTEffect {

    public enum Settings {
        DelayTime, Feedback, Type, Sync
    }

    // in seconds
    public static final float MAX_DELAY = 3.75f;
    public static final float MIN_DELAY = 0.15f;
    public static final float DEFAULT_TIME = .4f;
    static final float THRESHOLD = 0.00001f; // de-normalize

//    @Getter private boolean active;
    @Setter @Getter boolean sync;
    /** in seconds */
    private float delayTime;
    private float calculated; // delay in samples
    @Getter private float feedback = 0.36f;
    private final VariableDelayOp left;
    private final VariableDelayOp right;
    @Setter private boolean slapback;
    @Setter @Getter String type = TYPE[0];

    public Delay() {
        this(MAX_DELAY);
    }

    public Delay(float maxdelay) {
        int delayBufSize = (int) (maxdelay * SAMPLE_RATE) + 10;
        left = new VariableDelayOp(delayBufSize);
        right = new VariableDelayOp(delayBufSize);
        setDelayTime(DEFAULT_TIME);
        reset();
    }

//    @Override
//    public void setActive(boolean active) {
//        if (!active)
//            reset();
//        this.active = active;
//    }

    @Override
    public int getParamCount() {
        return Settings.values().length;
    }

    @Override public String getName() {
        return Delay.class.getSimpleName();
    }

    @Override
    public int get(int idx) {
        if (idx == Settings.DelayTime.ordinal())
            return Constants.reverseLog(delayTime, MIN_DELAY, MAX_DELAY);
        if (idx == Settings.Feedback.ordinal())
            return Math.round(getFeedback() * 100);
        if (idx == Settings.Type.ordinal())
            return TimeEffect.indexOf(type);
        if (idx == Settings.Sync.ordinal())
            return sync ? 1 : 0;
        throw new InvalidParameterException();
    }

    @Override
    public void set(int idx, int value) {
        if (idx == Settings.DelayTime.ordinal())
            setDelayTime(Constants.logarithmic(value, MIN_DELAY, MAX_DELAY));
        else if (idx == Settings.Feedback.ordinal())
            setFeedback(value / 100f);
        else if (idx == Settings.Type.ordinal() && value < TimeEffect.TYPE.length)
            type = TimeEffect.TYPE[value];
        else if (idx == Settings.Sync.ordinal())
            sync = value > 0;
        else throw new InvalidParameterException("" + idx);
    }

    public void setDelayTime(float msec) {
        delayTime = msec;
        calculated = delayTime * SAMPLE_RATE;
    }

    /** @return delay time in seconds */
    public float getDelay() {
        return delayTime;
    }

    public void setFeedback(float feedback) {
        if (feedback < 0 || feedback > 1) {
            throw new IllegalArgumentException("" + feedback);
        }
        this.feedback = feedback;
    }

    @Override
	public void reset() {
        if (left.workArea != null)
            Arrays.fill(left.workArea, 0);
        if (right.workArea != null)
            Arrays.fill(right.workArea, 0);
        // initialize internal delay state to the current target so we don't jump
        left.resetState(calculated);
        right.resetState(calculated);
    }

    @Override
    public void process(FloatBuffer inL, FloatBuffer inR) {
        left.process(inL);
        if (slapback) { // not implemented
            right.slapback(inL);
            return;
        }
        right.process(inR);
    }

    /**
     * - per-sample exponential smoothing of the read delay avoids abrupt jumps
     *   when the target delay changes (short or long).
     * - lastdelay stores the current smoothed read delay in samples.
     * - smoothingSamples controls the time-constant (in samples) of the smoothing.
     *   Larger values = slower, fewer artifacts; smaller = more responsive.
     * Not perfect but better*/
    private class VariableDelayOp {
        float[] workArea;
        int rovepos = 0;
        // current smoothed delay (in samples). Initialized in resetState().
        float lastdelay;
        // smoothing time constant: number of samples over which delay moves ~close to target.
        // You can raise this if you still hear clicks (but responsiveness falls).
        private static final int SMOOTHING_SAMPLES = 64;
        // derived smoothing coefficient (per-sample)
        private final float smoothAlpha = 1.0f / Math.max(1, SMOOTHING_SAMPLES);

        VariableDelayOp(int bufSize) {
            this.workArea = new float[bufSize];
            this.rovepos = 0;
            this.lastdelay = 0f;
        }

        void resetState(float initDelaySamples) {
            // initialize smoothing state to the current (target) delay to avoid jumps
            this.lastdelay = initDelaySamples;
            this.rovepos = 0;
        }

        void process(FloatBuffer in) {

            in.rewind();
            float ldelay = lastdelay; // smoothed delay (samples)
            float fb = feedback;
            float[] work = workArea;
            int rnlen = work.length;
            int pos = rovepos;

            // update ldelay per-sample with exponential smoothing towards
            // 'calculated' (the target delay in samples).
            float target = calculated;

            float r, s, a, b, o;
            int ri;
            float scratch;
            for (int i = 0; i < N_FRAMES; i++) {
                // smooth one sample towards target delay
                ldelay += (target - ldelay) * smoothAlpha;

                // compute read index for this (smoothed) delay
                r = pos - (ldelay + 2f) + rnlen;
                ri = (int) r;
                s = r - ri;

                // safe circular access (ri % rnlen)
                int idxA = ri % rnlen;
                if (idxA < 0) idxA += rnlen;
                int idxB = idxA + 1;
                if (idxB >= rnlen) idxB -= rnlen;

                a = work[idxA];
                b = work[idxB];
                o = a * (1 - s) + b * s;

                float inSample = in.get(i);

                // write feedback into buffer
                scratch = inSample + o;
                if (Math.abs(scratch) < THRESHOLD) // denormalize
                    scratch = 0f;
                work[pos] = scratch * fb;

                // write output (original wrote scratch back)
                in.put(scratch);

                pos++;
                if (pos >= rnlen) pos = 0;
            }
            // store smoothed delay and position for next block
            rovepos = pos;
            lastdelay = ldelay;
        }

        void slapback(FloatBuffer in) {
            in.rewind();
            float ldelay = lastdelay;
            float fb = feedback;
            int rnlen = workArea.length;
            int pos = rovepos;
            float target = calculated;

            float r, s, a, b, o;
            int ri;
            for (int i = 0; i < N_FRAMES; i++) {
                ldelay += (target - ldelay) * smoothAlpha;

                r = pos - (ldelay + 2f) + rnlen;
                ri = (int) r;
                s = r - ri;

                int idxA = ri % rnlen;
                if (idxA < 0) idxA += rnlen;
                int idxB = idxA + 1;
                if (idxB >= rnlen) idxB -= rnlen;

                a = workArea[idxA];
                b = workArea[idxB];
                o = a * (1 - s) + b * s;

                float outSample = o;
                workArea[pos] = in.get(i) + outSample * fb;
                in.put(outSample);

                pos++;
                if (pos >= rnlen) pos = 0;
            }

            rovepos = pos;
            lastdelay = ldelay;
        }
    }

    @Override
    public void sync(float unit) {
        float msec = 0.001f * (unit + unit * TimeEffect.indexOf(type));
        setDelayTime(2 * msec);
    }

}


/* TODO Stereo Delay
Width / spatialization — offsetting left/right delay times or levels gives a sense of stereo spread and depth. Small offsets (a few ms) produce a stereo image without obvious echoes.
Ping‑pong / bouncing delays — alternating echoes L→R→L create movement and interest in the stereo field.
Decorrelation / richness — slightly different delay times, filters, or modulation on each channel avoid comb filtering and make the repeats sound more natural and lush.
Separate tonality per side — independent lowpass/highpass/EQ on each channel lets you shape the repeats so they don’t build up harshness.
Complex textures via cross‑feedback — feeding a portion of left output into the right delay (and vice versa) yields evolving stereo ambience and dense rhythmic patterns.
Haas / precedence effects — very short offsets (< 30–40 ms) create perceived spatial position without distinct echoes (useful for panning effects).
Tempo and rhythmic interest — one side synced to the beat, the other offset or dotted/tri‑sync creates stereo rhythm.
Mid/Side (M/S) processing — delay only the sides (or center) to preserve mono compatibility and still create width.
Implementation techniques (what to add)

Independent L/R delayTime and feedback controls — let the two channels differ.
Cross‑feedback control (0..1) — route a fraction of L output into R input and vice versa for ping‑pong or cross‑ambient modes.
Ping‑pong mode — when active, route the output of one delay into the other with timing so echoes alternate.
Per‑channel filters on feedback path — 1‑pole low/highpass (or small EQ) to tame HF build-up and reduce ringing.
Small detune / modulation differences — LFOs that are slightly out of phase or different speeds produce decorrelation.
Stereo width param — interpolate between fully independent delays and fully identical delays (or between in-phase and inverted-phase M/S).
Mono-safe / sum-to-mono option — avoid large L/R differences that cancel when summed. Provide center-preserve mode or M/S-safe processing.
Smoothing of parameter changes — keep per-sample smoothing for delay changes (you already use this in your nicer fix) to avoid clicks when altering times or extending buffers.
Minimal design variants (examples)

True stereo independent:
Keep left/right VariableDelayOp, allow independent delayTimeLeft/delayTimeRight, feedbackLeft/feedbackRight. No cross-feed.
Cross‑feedback / ping‑pong:
leftInput = inL + rightOut * crossFB
rightInput = inR + leftOut * crossFB
Where crossFB = crossFeedback * feedback (tweak scaling to avoid runaway).
Ping‑pong simpler:
left.write = in + rightOut * fb; right.write = in + leftOut * fb; and set one side to output only the other side’s echoes.
Haas widen:
Keep very small rightDelay = leftDelay + 5..30ms and low feedback. No cross‑feed, just offset times.
M/S-mode:
Convert L/R input to M/S, delay S channel, convert back. This preserves mono while widening sides.
Practical parameter suggestions

Small stereo spread (natural): rightDelay = leftDelay ± 5–25 ms
Haas (positioning): 5–30 ms offset, low feedback
Ping‑pong: crossFB 0.6–0.95, per‑channel feedback 0.3–0.6 (watch for instability)
Per-channel LPF on feedback: 1‑pole with fc around 6–8 kHz (or fbCut around 0.1–0.3 depending on normalized coefficient)
Cross‑feedback limit: clamp crossFB*feedback < 0.95 to avoid runaway gain
Pitfalls & what to watch for

Mono-sum phase cancellation — big L/R time/phase differences can cancel when summed to mono. Offer a mono-sum preview or a “mono-safe” option (M/S or center-preserve).
Feedback runaway — cross-feedback plus high feedback can cause instability. Add clamping and maybe a soft limiter on the feedback path or global safety clamp.
CPU / memory — two buffers and cross-routing are modestly heavier but still cheap; be mindful if you add many taps/modulation LFOs.
Clicks/artifacts — you already fixed per-sample smoothing for delay changes; do the same for per-channel delay changes and cross-feed level changes.
Minimal concrete changes you can add to your Delay class now (1) Cross‑feedback/ping‑pong knobs:

new fields: float crossFeedback = 0f; boolean pingPong = false;
in VariableDelayOp.process, accept an external cross-in value and write work[pos] = in + (o + crossIn) * fb (or use cross routing in Delay.process)
(2) Per-channel one‑pole LPF on feedback (reduce ringing):

add fbFilterStateLeft/Right and fbCut (as you added in Chorus). Filter the o * fb before writing into buffer.
(3) Independent delay times:

add setDelayLeft/setDelayRight (or pass left/right target delays) and store calculatedLeft/calculatedRight; ensure smoothing per-channel.
(4) Ping‑pong implementation sketch:

In process(inL, inR) do:
leftOut = left.processInline(inL, crossInFromRight);
rightOut = right.processInline(inR, crossInFromLeft);
If pingPong: feed leftOut into right’s feedback input and rightOut into left’s feedback input (scale by crossFeedback).
Write outputs accordingly. */
