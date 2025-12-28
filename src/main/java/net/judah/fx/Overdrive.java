package net.judah.fx;

import java.nio.FloatBuffer;
import java.security.InvalidParameterException;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.judah.api.Effect.RTEffect;
import net.judah.util.Constants;


/** See: https://github.com/martinpenberthy/JUCEGuitarAmpBasic
 *  See: https://github.com/jaudiolibs/pipes/blob/master/pipes-units/src/main/java/org/jaudiolibs/pipes/units/Overdrive.java */
public final class Overdrive implements RTEffect {
    static final float MIN_DRIVE = 0.1f;
    static final float MAX_DRIVE = 0.9f;

    public enum Settings { Drive, Clipping, Algo }

    @RequiredArgsConstructor
    public enum Algo {
        SOFT(1.15f),    // x / (1 + |x|), hardness depends on drive
        HARD(1.15f),    // SOFT++, more hardness with drive
        BLUE(0.23f),    // blues-style asymmetric tanh pair
        SMITH(0.34f),   // atan (legacy flavour)
        ZONE(0.37f),    // x / (abs(x) + k), k depends on drive
        MESA(0.55f),    // exponential boogie-style
        TUBE(1f),       // S-curve tube screamer
        TWIN(0.15f),    // AMP1-style tube-ish curve
        FUZZ(0.23f),    // AMP2-style, hardness follows drive
        FOLD(0.7f);     // foldback glitch for synths

        private final float makeupGain;
    }

    /** Per‑sample waveshaper interface so we can avoid a switch in the inner loop. */
    @FunctionalInterface
    private interface Waveshaper {
        float apply(float x);
    }

    @Getter private final String name = Overdrive.class.getSimpleName();
    @Getter private final int paramCount = Settings.values().length;
//    @Getter private boolean active;

    /** 0..1 logarithmic */ // 0.1 .. 0.8
    private float drive = 0.28f;
    /** clipping stage - user */
    private int clipping = 0;
    /** clipping stage - calculated */
    private float diode = 2f;
    private Algo algo = Algo.SMITH;
    /** Cached per‑sample function built whenever drive or algo changes. */
    private Waveshaper shaper = x -> x; // rebuildShaper()
    private static final float SAFETY_OUTPUT_CLAMP = 0.995f; // clamp outputs to +/-1.0


    @Override public int get(int idx) {
        return switch (idx) {
            case 0 -> {
                if (drive < 0.00002f)
                    yield 0;
                if (drive < 0.021f)
                    yield 1;
                yield Constants.reverseLog(drive, MIN_DRIVE, MAX_DRIVE);
            }
            case 1 -> clipping;
            case 2 -> algo.ordinal();
            default -> throw new InvalidParameterException("Setting " + idx);
        };
    }

    @Override public void set(int idx, int value) {
        switch (idx) {
            case 0 -> {
                if (value == 0)
                    drive = 0.00001f;
                else if (value == 1)
                    drive = 0.02f;
                else
                    drive = Constants.logarithmic(value, MIN_DRIVE, MAX_DRIVE);
                activate();
            }
            case 1 -> {
                // clipping 0..100 -> 0..1
                clipping = value;
                // as clipping -> 0, diode -> 3; as clipping -> 1, diode -> 1
                diode = 1f + (3f - 2.7f * (0.01f * clipping));
            }
            case 2 -> {
                Algo[] algos = Algo.values();
                int clamped = Math.max(0, Math.min(value, algos.length - 1));
                algo = algos[clamped];
                activate();
            }
            default -> throw new InvalidParameterException("Setting " + idx + " (=" + value + ")");
        }
    }

    /**Rebuild per‑sample waveshaper based on current drive and algorithm.
     * Attempt to tie hardness/harmonic content to the drive parameter. */
    @Override
	public void activate() {

        // friendlier range, roughly [1 .. 30]
        final float driveGain = 1f + drive * 29f;

        switch (algo) {
            case SMITH -> { // Classic arctan transfer plus drive
                // Copyright 2019 Neil C Smith.  JAudioLibs
                double preMulD = drive * 99 + 1;
                double postMulD = 1 / (Math.log(preMulD * 2) * 1.0 / Math.log(2)/*LOG2*/);
                final float preMul = (float) preMulD;
                final float postMul = (float) postMulD;
                shaper = x -> (float) (Math.atan(x * preMul) * postMul);
            }

            case BLUE -> { // Asymmetric tanh pair (tamed)
                final float driveShaped = (float) Math.pow(drive, 1.2f);   // same shaping
                final float posGain = 3f + 12f * driveShaped;              // 3..15
                final float negGain = 1.5f + 6f * driveShaped;             // 1.5..7.5
                final float posLevel = 0.9f + 0.45f * driveShaped;
                final float negLevel = 0.7f + 0.35f * driveShaped;

                // safety caps to prevent extreme values at the very top
                final float posGainCapped = Math.min(posGain, 14f);
                final float negGainCapped = Math.min(negGain, 8f);

                shaper = x -> {
                    if (x >= 0f) {
                        // hotter, more compressed positive lobe
                        return (float) (Math.tanh(posGainCapped * x) * posLevel);
                    } else {
                        // softer, but still driven negative lobe
                        return (float) (Math.tanh(negGainCapped * x) * negLevel);
                    }};}

            case TWIN -> { // "hardness" and level follow drive
                final float kDrive = 0.9f + 0.3f * drive;                // 0.9 .. 1.2
                final float globalScale = 0.7f * (0.7f + 0.4f * drive);  // 0.49..0.77
                shaper = x -> {
                    float ax = Math.abs(x);
                    float denom1 = ax + kDrive;
                    if (denom1 == 0f) return 0f;
                    float num = (x / denom1) * 1.5f * driveGain;
                    float denom2 = x * x + (-1.0f) * ax + 1.0f;
                    if (denom2 == 0f) return 0f;
                    return (num / denom2) * globalScale;
                };}

            case ZONE -> { // x / (abs(x) + k), where k shrinks with drive, more saturated as drive increases.
                final float baseK = 2.0f;
                final float k = baseK - 0.9f * drive; // 2.0 .. 1.1
                shaper = x -> {
                    float ax = Math.abs(x);
                    float denom = ax + k;
                    if (denom == 0f) return 0f;
                    return (x / denom) * driveGain * 0.5f;
                };}

            case FUZZ -> { // AMP2-style: hardness and level follow drive.
                final float kDrive = 0.9f + 0.4f * drive;                // 0.9 .. 1.3
                final float globalScale = 0.6f * (0.8f + 0.35f * drive); // 0.48..0.714
                shaper = x -> {
                    float ax = Math.abs(x);
                    if (ax == 0f) return 0f;
                    float num = x * (ax + kDrive) * 1.5f * driveGain;
                    float denom = x * x + 0.3f * (0.1f / ax) + 1.0f;
                    if (denom == 0f) return 0f;
                    return (num / denom) * globalScale;
                };}

            case SOFT -> { // Soft version: x / (1 + |x| * h), with h tied to drive.
                final float hardness = 0.7f + 2.3f * drive; // 0.7 .. 3.0
                shaper = x -> {
                    float ax = Math.abs(x);
                    float denom = 1.0f + ax * hardness;
                    if (denom == 0f) return 0f;
                    return (x / denom) * (0.8f + 0.6f * drive);
                };}

            case HARD -> { // SOFT++
                final float driveShaped = (float) Math.pow(drive, 1.3);   // gentle at low drive, strong near 1
                final float hardness    = 1.0f + 9.0f * driveShaped;      // 1 .. ~10
                final float outGain     = 0.7f + 1.0f * driveShaped;      // 0.7 .. 1.7
                shaper = x -> {
                    float ax = Math.abs(x);
                    float denom = 1.0f + ax * hardness;
                    if (denom == 0f) return 0f;
                    return (x / denom) * outGain;
                };}

            case MESA -> { // Exponential soft clip, mid-gain
                final float a = 1f + 9f * drive;
                final float norm = (float) (1.0 - Math.exp(-a)); // normalize at x=1
                final float driveMix = 0.4f + 0.6f * drive;      // wet/dry balance
                shaper = x -> {
                    // raw exp curve
                    float v = (float) ((1.0 - Math.exp(-a * x)) / norm);
                    // blend with dry to keep some body
                    return (1f - driveMix) * x + driveMix * v;
                };}

            case TUBE -> { // S-curve saturator
                final float shapeGain = 0.7f + 0.9f * drive; // 0.7..1.6
                shaper = x -> {
                    float ax = Math.abs(x);
                    float denom = 2f + ax;
                    if (denom == 0f) return 0f;
                    float s = 3f * x / denom;
                    float mix = 0.5f + 0.4f * drive; // 0.5..0.9
                    float wet = s * shapeGain;
                    return (1f - mix) * x + mix * wet;
                };}

            case FOLD -> { // Foldback: more gain & craziness with drive.
                // symmetric around zero (no big DC bias)
                final float foldGain = 1f + 4f * drive;    // 1..5
                final float k = 0.4f;                      // fold range
                final float dryMix = 0.5f - 0.3f * drive;  // 0.5 -> 0.2
                final float wetMix = 1f - dryMix;         // 0.5 -> 0.8
                final float range = 2f * k;
                final float limit = SAFETY_OUTPUT_CLAMP;

                shaper = x -> {
                    float v = x * foldGain;
                    // modulo fold into [-k, k)
                    float t = (v + k) % range;
                    if (t < 0f) t += range;
                    float folded = t - k; // now in [-k, k)
                    float y = dryMix * x + wetMix * folded;

                    // hard safety, branchless-ish
                    y = Math.max(-limit, Math.min(limit, y));
                    return y;
                };}

            default -> {
                shaper = x -> x;
            }
        }
    }

    @Override public void process(FloatBuffer left, FloatBuffer right) {
        process(left, true);
        process(right, false);
    }

    /** Process 1 channel:
     *  - shape with algo
     *  - makeup gain
     *  - apply diode clipping
     *  - soft safety clamp + light DC block per-channel */
    public void process(FloatBuffer buf, boolean isLeft) {
        final Waveshaper waveShaper = shaper;
        final float algoGain = algo.makeupGain;

        buf.rewind();
        if (clipping == 0) {
            // No clipping: just shape + gain
            while (buf.hasRemaining()) {
            	float y = waveShaper.apply(buf.get()) * algoGain;
                y = Math.max(-SAFETY_OUTPUT_CLAMP, Math.min(SAFETY_OUTPUT_CLAMP, y));
                buf.put(buf.position() - 1, y);
            }
        } else { // include clipping diode stage
            final float localDiode = this.diode;
            while (buf.hasRemaining()) {
                float x = buf.get();
                float y = waveShaper.apply(x) * algoGain;

                // diode limiting
                float max = localDiode * x;
                // if y exceeds |max|: clip
                if (Math.abs(y) > Math.abs(max))
                    y = max;
                buf.put(buf.position() - 1, y);
            }
        }
    }
}

// This DC can blow up your machine
//case RECT -> { // Half-rectifier whose effective threshold follows drive.
//    // Make rect less DC-producing and cap its local gain so it can't explode.
//    final float rectGain = 0.4f + 0.8f * drive; // 0.4 .. 1.2
//    // scale back by part of driveGain to keep energy reasonable
//    final float localScale = 0.95f / Math.max(1f, driveGain * rectGain);
//    shaper = x -> {
//        float y = x < 0f ? 0f : x;
//        y = y * rectGain * driveGain * 0.5f * localScale;
//        // small soft protection: avoid producing large outputs from one stage
//        if (y > SAFETY_OUTPUT_CLAMP) y = SAFETY_OUTPUT_CLAMP;
//        if (y < -SAFETY_OUTPUT_CLAMP) y = -SAFETY_OUTPUT_CLAMP;
//        return y;
//    };}
// Simple per-channel DC trackers for a light DC-blocking step after shaping.
// keep small alpha so we don't alter audio, just remove slow DC bias.
// private float dcLeft = 0f;
// private float dcRight = 0f;
// private static final float DC_ALPHA = 1e-4f; // small -> slow
// light DC estimator & remove (simple leaky average)
//    if (isLeft) {
//        dcLeft += DC_ALPHA * (y - dcLeft);
//        y -= dcLeft;
//    } else {
//        dcRight += DC_ALPHA * (y - dcRight);
//        y -= dcRight;
//    }

