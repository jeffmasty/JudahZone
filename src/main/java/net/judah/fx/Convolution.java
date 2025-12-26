package net.judah.fx;

import java.nio.FloatBuffer;
import java.security.InvalidParameterException;
import java.util.Arrays;

import be.tarsos.dsp.util.fft.FFT;
import lombok.Getter;
import net.judah.JudahZone;
import net.judah.omni.AudioTools;
import net.judah.util.Constants;
import net.judah.util.RTLogger;

public abstract class Convolution implements Effect {

    public enum Settings { Cabinet, Wet}
    protected static final IRDB db = JudahZone.getIR();

    @Getter protected final String name = Convolution.class.getSimpleName();
    @Getter protected final int paramCount = Settings.values().length;

    // ======================================================================
    /**Stereo wrapper: Double barrel, side-by-side, come to tan your hide. */
    public static class Stereo extends Convolution {

        private final Mono leftIR = new Mono();
        private final Mono rightIR = new Mono();

        @Override public void setActive(boolean active) {
            leftIR.setActive(active);
            rightIR.setActive(active);
        }

		@Override public boolean isActive() {
			return leftIR.isActive();
		}

        @Override public void set(int idx, int value) {
            leftIR.set(idx, value);
            rightIR.set(idx, value);
        }

        @Override public void process(FloatBuffer l, FloatBuffer r) {
        	leftIR.process(l);
        	rightIR.process(r);
        }

		@Override public int get(int idx) {
			return leftIR.get(idx);
		}

    }

    // ======================================================================
    /** MONO: Convolute a selected IR against live audio */
    public static class Mono extends Convolution {

        protected static final int FFT_SIZE = Constants.fftSize();
        protected final FFT fft = new FFT(FFT_SIZE);
        protected final FFT ifft = new FFT(FFT_SIZE);

        protected final int overlapSize = FFT_SIZE - N_FRAMES;

        // pointer to currently selected IR spectrum (from DB)
        protected float[] irFreq = new float[FFT_SIZE * 2];
        @Getter protected boolean active;
        protected float wet = 0.9f;
        protected int cabinet = 0;

        // instance working buffers (allocated once)
        protected final float[] fftInOut = new float[FFT_SIZE * 2];
        protected final float[] overlap = new float[overlapSize];
        protected final float[] work0 = new float[N_FRAMES];
        protected final float[] work1 = new float[N_FRAMES];

        public Mono() {
            if (db != null && db.size() > 0) {
                cabinet = 0;
                irFreq = db.get(0).irFreq();
            }
            reset();
        }

        protected void reset() {
            Arrays.fill(overlap, 0f);
        }

        @Override public void setActive(boolean active) {
    		this.active = active;
    		if (!active)
    			reset();
    	}

        @Override public void set(int idx, int value) {
            if (idx == Settings.Cabinet.ordinal()) {
                if (db.size() == 0) {
                    RTLogger.warn(this, "No cabinets loaded");
                    return;
                }
                if (value < 0 || value >= db.size()) {
                    throw new InvalidParameterException("Cabinet index out of range: " + value);
                }
                cabinet = value;
                irFreq = db.get(cabinet).irFreq();
                reset();
                return;
            }

            if (idx == Settings.Wet.ordinal()) {
                // map 0..100 -> 0.0..1.0
                wet = value * 0.01f;
                if (wet > 1) wet = 1;
                if (wet < 0) wet = 0;
                return;
            }
            throw new InvalidParameterException("Unknown param index: " + idx);
        }

        @Override public int get(int idx) {
            if (idx == Settings.Cabinet.ordinal()) {
                return cabinet;
            }
            if (idx == Settings.Wet.ordinal()) {
                return Math.round(wet * 100f);
            }
            throw new InvalidParameterException("Unknown param index: " + idx);
        }


        /** Convolve Add and make stereo, even if dry/inactive */
        public void monoToStereo(FloatBuffer mono, FloatBuffer stereo) {
            if (!active || wet <= 0f) {
            	AudioTools.copy(mono, stereo); // Instrument expects stereo out of here
            	return;
            }
            float dryGain = 1.0f - wet;
            float wetGain = wet;

            // Read input block without disturbing caller's buffer position
            FloatBuffer inBuf = mono.duplicate();
            inBuf.rewind();
            inBuf.get(work0, 0, N_FRAMES);

            // Prepare FFT input (real time samples in indices 0..FFT_SIZE-1)
            float[] fftInOut = new float[FFT_SIZE * 2];
            Arrays.fill(fftInOut, 0f);

            // Compose the overlap-save input: [previous overlap | new block]
            // overlap[] length = FFT_SIZE - N_FRAMES
            System.arraycopy(overlap, 0, fftInOut, 0, overlapSize);
            System.arraycopy(work0, 0, fftInOut, overlapSize, N_FRAMES);

            // Save next overlap for the following block: last (FFT_SIZE - N_FRAMES) samples of this composite input
            // These are at positions [N_FRAMES .. FFT_SIZE-1] of fftInOut (real domain)
            System.arraycopy(fftInOut, N_FRAMES, overlap, 0, overlapSize);

            // Forward FFT (in-place, produces complex interleaved in fftInOut)
            fft.forwardTransform(fftInOut);

            // Complex multiply with IR spectrum (irFreq is complex interleaved)
            // For each bin k: (a + jb) * (c + jd) = (ac - bd) + j(ad + bc)

            for (int k = 0, idx = 0; k < FFT_SIZE; k++, idx += 2) {
                float a = fftInOut[idx];
                float b = fftInOut[idx + 1];
                float c = irFreq[idx];
                float d = irFreq[idx + 1];
                float real = a * c - b * d;
                float imag = a * d + b * c;
                fftInOut[idx] = real;
                fftInOut[idx + 1] = imag;
            }

            // Inverse FFT -> time domain (real samples in indices 0..FFT_SIZE-1)
            ifft.backwardsTransform(fftInOut);

            // Extract valid linear-convolution output: indices overlapSize .. overlapSize + N_FRAMES - 1
            // Mix with dry signal and write back to mono (and optionally to ignore)
            FloatBuffer outBuf = mono.duplicate();
            outBuf.rewind();

            // If ignore buffer provided, write there as well (do not change its position in caller)
            FloatBuffer stereoOut = stereo.duplicate();
            stereoOut.rewind();

            for (int i = 0; i < N_FRAMES; i++) {
                float proc = fftInOut[overlapSize + i]; // processed (wet) sample
                float in = work0[i];                  // original (dry) sample
                float mixed = dryGain * in + wetGain * proc;
                work1[i] = mixed;
            }

            // Write mixed result back into buffers
            outBuf.put(work1, 0, N_FRAMES);
            stereoOut.put(work1, 0, N_FRAMES);

        }

        /** Realtime Audio  Convolve Add */
        public void process(FloatBuffer mono) {
            // If inactive or fully dry, preserve caller expectation of stereo output.
            if (!active)
                return;

            final float dryGain = 1.0f - wet;
            final float wetGain = wet;

            // Read input block without disturbing caller's buffer position
            FloatBuffer inBuf = mono.duplicate();
            inBuf.rewind();
            inBuf.get(work0, 0, N_FRAMES);

            // Prepare FFT input (real time samples in indices 0..FFT_SIZE-1)
            float[] fftInOut = new float[FFT_SIZE * 2];
            Arrays.fill(fftInOut, 0f);

            // Compose the overlap-save input: [previous overlap | new block]
            System.arraycopy(overlap, 0, fftInOut, 0, overlapSize);
            System.arraycopy(work0, 0, fftInOut, overlapSize, N_FRAMES);

            // Save next overlap for the following block: last (FFT_SIZE - N_FRAMES) samples of this composite input
            System.arraycopy(fftInOut, N_FRAMES, overlap, 0, overlapSize);

            // Forward FFT (in-place, produces complex interleaved in fftInOut)
            fft.forwardTransform(fftInOut);

            // Complex multiply with IR spectrum (irFreq is complex interleaved)
            for (int k = 0, idx = 0; k < FFT_SIZE; k++, idx += 2) {
                float a = fftInOut[idx];
                float b = fftInOut[idx + 1];
                float c = irFreq[idx];
                float d = irFreq[idx + 1];
                float real = a * c - b * d;
                float imag = a * d + b * c;
                fftInOut[idx] = real;
                fftInOut[idx + 1] = imag;
            }

            // Inverse FFT -> time domain (real samples in indices 0..FFT_SIZE-1)
            ifft.backwardsTransform(fftInOut);

            // Mix wet/dry into work1 buffer
            for (int i = 0; i < N_FRAMES; i++) {
                float proc = fftInOut[overlapSize + i]; // processed (wet) sample
                float in = work0[i];                    // original (dry) sample
                work1[i] = dryGain * in + wetGain * proc;
            }

            // Write mixed result back into mono buffer
            FloatBuffer outBuf = mono.duplicate();
            outBuf.rewind();
            outBuf.put(work1, 0, N_FRAMES);

        }
    }

    @Override public void process(FloatBuffer left, FloatBuffer right) {
    	// no-op
    }



}