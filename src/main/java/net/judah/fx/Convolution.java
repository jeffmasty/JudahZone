package net.judah.fx;

import static judahzone.util.WavConstants.FFT_SIZE;

import java.nio.FloatBuffer;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import be.tarsos.dsp.util.fft.FFT;
import judahzone.api.Effect;
import judahzone.util.AudioTools;
import judahzone.util.RTLogger;
import lombok.Getter;

public abstract class Convolution implements Effect {

    public enum Settings { Cabinet, Wet}
    private static List<String> names = settingNames();
    private static List<String> settingNames() {
    	ArrayList<String> build = new ArrayList<String>();
    	for (Settings s : Settings.values())
    		build.add(s.name());
    	return Collections.unmodifiableList(build);
    }

    protected static IRProvider db;
	public static void setIRDB(IRProvider provider) { db = provider; }


    @Getter protected final String name = Convolution.class.getSimpleName();
    @Getter protected final int paramCount = Settings.values().length;
    @Override public List<String> getSettingNames() { return names; }

    // ======================================================================
    /** Wrapper around 2 Mono Convolvers */
    public static class Stereo extends Convolution implements RTEffect {

        private final Mono leftIR = new Mono();
        private final Mono rightIR = new Mono();

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

        protected final FFT fft = new FFT(FFT_SIZE);
        protected final FFT ifft = new FFT(FFT_SIZE);

        protected final int overlapSize = FFT_SIZE - N_FRAMES;

        // pointer to currently selected IR spectrum (from DB)
        protected float[] irFreq = new float[FFT_SIZE * 2];
        protected float wet = 0.9f;
        protected int cabinet = -1; // lazy load

        // instance working buffers (allocated once)
        protected final float[] fftInOut = new float[FFT_SIZE * 2];
        protected final float[] overlap = new float[overlapSize];
        protected final float[] work0 = new float[N_FRAMES];
        protected final float[] work1 = new float[N_FRAMES];

        @Override public void reset() {
            Arrays.fill(overlap, 0f);
        }

        @Override public void set(int idx, int value) {
            if (idx == Settings.Cabinet.ordinal()) {
            	if (db == null) {
            		RTLogger.warn(this, "No IRDB set");
            		return;
            	}
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

        @Override
        public void activate() {
        	if (cabinet < 0)  // first time (DB allowed to load)
        		if (db != null) // if null user gets zeros
        			set(Settings.Cabinet.ordinal(), 0);
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
            if (wet <= 0f) {
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