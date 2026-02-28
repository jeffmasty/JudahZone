// java
package net.judah.drums.pluck;

import java.util.Arrays;

/** Immutable, RT-safe snapshot of pluck parameters (per-drum arrays). */
public final class PluckParams {
    public final float[] loopGain;
    public final float[] fbCut;
    public final float[] tap;
    public final float[] pitchHz;
    public final int[] samples;
    public final float[] amp;

    public PluckParams(float[] loopGain, float[] fbCut, float[] tap, float[] pitchHz, int[] samples, float[] amp) {
        this.loopGain = loopGain != null ? Arrays.copyOf(loopGain, loopGain.length) : new float[0];
        this.fbCut = fbCut != null ? Arrays.copyOf(fbCut, fbCut.length) : new float[0];
        this.tap = tap != null ? Arrays.copyOf(tap, tap.length) : new float[0];
        this.pitchHz = pitchHz != null ? Arrays.copyOf(pitchHz, pitchHz.length) : new float[0];
        this.samples = samples != null ? Arrays.copyOf(samples, samples.length) : new int[0];
        this.amp = amp != null ? Arrays.copyOf(amp, amp.length) : new float[0];
    }

    public static PluckParams empty(int voices) {
        return new PluckParams(new float[voices], new float[voices], new float[voices], new float[voices], new int[voices], new float[voices]);
    }

    public PluckParams withLoopGain(int idx, float value) {
        float[] lg = Arrays.copyOf(loopGain, loopGain.length);
        lg[idx] = value;
        return new PluckParams(lg, fbCut, tap, pitchHz, samples, amp);
    }

    public PluckParams withFbCut(int idx, float value) {
        float[] fb = Arrays.copyOf(fbCut, fbCut.length);
        fb[idx] = value;
        return new PluckParams(loopGain, fb, tap, pitchHz, samples, amp);
    }

    public PluckParams withTap(int idx, float value) {
        float[] t = Arrays.copyOf(tap, tap.length);
        t[idx] = value;
        return new PluckParams(loopGain, fbCut, t, pitchHz, samples, amp);
    }

    public PluckParams withPitchHz(int idx, float hz) {
        float[] p = Arrays.copyOf(pitchHz, pitchHz.length);
        p[idx] = hz;
        return new PluckParams(loopGain, fbCut, tap, p, samples, amp);
    }

    public PluckParams withSamples(int idx, int s) {
        int[] ss = Arrays.copyOf(samples, samples.length);
        ss[idx] = s;
        return new PluckParams(loopGain, fbCut, tap, pitchHz, ss, amp);
    }

    public PluckParams withAmp(int idx, float a) {
        float[] aa = Arrays.copyOf(amp, amp.length);
        aa[idx] = a;
        return new PluckParams(loopGain, fbCut, tap, pitchHz, samples, aa);
    }
}

