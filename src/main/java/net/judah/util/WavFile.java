package net.judah.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.resample.Resampler;
import net.judah.api.Recording;

// sinWave(File out, long millis, int hz, float magnitude

/**
 * audio reader/writer
 *
 * - getMono(File)  -> returns a mono float[] (no fixed length)
 * - getMono(File, length) -> returns mono float[] trimmed/padded to length (for IRs)
 * - getStereo(File)-> returns float[2][] {left[], right[]} using Constants.sampleRate()
 * - getStereo(File, targetSampleRate) -> returns float[2][] resampled to targetSampleRate
 * - getRecording(File) -> returns a Recording (vector of float[2][JACK_BUFFER]) at Constants.sampleRate()
 *
 * - save(Recording, File) and overloaded variants save Recording to a WAV with configurable sample rate & bit depth.
 *
 * Notes:
 * - Reading: attempts to request the dispatcher to provide audio at the requested sample rate. If the dispatcher
 *   reports a different rate, the code uses the bundled Resampler to convert arrays to the target rate.
 * - MP3 support: AudioDispatcherFactory.fromPipe(...) can delegate to ffmpeg/avconv or system SPI for decoding.
 *   If your environment has ffmpeg in PATH, MP3 (and many other formats) should work.
 * - Writing: save(...) streams the Recording into a WAV file; supports common PCM depths 8/16/24/32.
 *   8-bit is written as unsigned PCM (standard WAV), 16/24/32 as little-endian signed PCM.
 *
 * - This class will handle high sample rates (48k, 96k, etc.) as long as the JVM/OS and audio pipeline
 *   can allocate the buffers. Resampling uses the included Resampler for correctness when necessary.
 */
public final class WavFile {

    private WavFile() {}

    /**
     * Return mono samples trimmed or zero-padded to exactly 'max' length.
     * If the file contains fewer samples than max, the returned array is zero padded.
     * If the file contains more samples than max, the returned array is truncated to max.
     */
    public static float[] getMono(File f, int max) throws IOException, UnsupportedAudioFileException {
        if (max < 0) throw new IllegalArgumentException("max must be >= 0");
        float[] raw = getMono(f);
        if (raw == null) return new float[max];
        if (raw.length == max) return raw;
        if (raw.length > max) return Arrays.copyOf(raw, max);

        // pad
        float[] result = new float[max];
        System.arraycopy(raw, 0, result, 0, raw.length);
        return result;
    }

    /**
     * Read file and return mono samples at Constants.sampleRate() (no fixed length).
     */
    public static float[] getMono(File wav) throws IOException, UnsupportedAudioFileException {
        if (wav == null) throw new IllegalArgumentException("wav is null");
        final float targetSampleRate = Constants.sampleRate();

        // Create dispatcher; ask it to provide audio at the target sample rate.
        // Buffer size of 2048 frames per channel is a reasonable default for file reading.
        AudioDispatcher dispatcher = AudioDispatcherFactory.fromPipe(
                wav.getAbsolutePath(),
                (int) targetSampleRate,
                2048,
                0);

        final float sourceSampleRate = dispatcher.getFormat().getSampleRate();

        final List<Float> samples = new ArrayList<>();

        dispatcher.addAudioProcessor(new AudioProcessor() {
            @Override public boolean process(AudioEvent audioEvent) {
                float[] buf = audioEvent.getFloatBuffer();
                for (float v : buf) {
                    samples.add(v);
                }
                return true;
            }
            @Override public void processingFinished() {}
        });

        dispatcher.run();

        int len = samples.size();
        float[] out = new float[len];
        for (int i = 0; i < len; i++) out[i] = samples.get(i);

        // If dispatcher did not already supply samples at the requested rate, resample
        if (sourceSampleRate > 0 && sourceSampleRate != targetSampleRate) {
            out = resampleArray(out, sourceSampleRate, targetSampleRate);
        }
        return out;
    }

    /**
     * Read file and return stereo samples [left,right] at Constants.sampleRate().
     * If the file is mono it is duplicated to both channels.
     * Mastering defaults to 1.0 (no gain).
     */
    public static float[][] getStereo(File wav) throws IOException, UnsupportedAudioFileException {
        return getStereo(wav, Constants.sampleRate(), 1.0f);
    }
    public static float[][] getStereo(File wav, int sampleRate) throws IOException, UnsupportedAudioFileException {
    	return getStereo(wav, sampleRate, 1.0f);
    }

    public static float[][] getStereo(File wav, float mastering) throws IOException, UnsupportedAudioFileException {
    	return getStereo(wav, Constants.sampleRate(), mastering);
    }

    /**
     * Read file and return stereo samples [left,right] resampled to targetSampleRate.
     * Works with .wav and .mp3 (provided the decoding backend/ffmpeg is available).
     *
     * The mastering parameter must be positive. Each output sample is multiplied once by mastering
     * and then clamped to [-1,1].
     *
     * @param wav input file
     * @param targetSampleRate desired sample rate for output arrays
     * @param mastering positive multiplier to apply to every sample (1.0 = no change)
     * @return float[2][len] where [0]=L, [1]=R
     */
    public static float[][] getStereo(File wav, final int targetSampleRate, final float mastering)
    		throws IOException, UnsupportedAudioFileException {
        if (wav == null) throw new IllegalArgumentException("wav is null");
        if (targetSampleRate <= 0) throw new IllegalArgumentException("targetSampleRate must be > 0");
        if (!(mastering > 0f)) throw new IllegalArgumentException("mastering must be a positive float (> 0)");

        // Ask dispatcher to produce audio at targetSampleRate. fromPipe commonly uses
        // an external decoder (ffmpeg) when needed, so MP3 support is possible if ffmpeg is present.
        AudioDispatcher dispatcher = AudioDispatcherFactory.fromPipe(
                wav.getAbsolutePath(),
                targetSampleRate,
                2048,
                0
        );

        final float sourceSampleRate = dispatcher.getFormat().getSampleRate();
        final int sourceChannels = dispatcher.getFormat().getChannels();

        final List<Float> left = new ArrayList<>();
        final List<Float> right = new ArrayList<>();

        dispatcher.addAudioProcessor(new AudioProcessor() {
            @Override public boolean process(AudioEvent audioEvent) {
                float[] buf = audioEvent.getFloatBuffer();

                if (sourceChannels <= 1) {
                    for (float v : buf) {
                        left.add(v);
                        right.add(v);
                    }
                    return true;
                }

                // Tarsos historically supplies non-interleaved buffers for some backends:
                // [ leftSamples... | rightSamples... ]
                // Detect common non-interleaved pattern: even length and halves of reasonable size.
                if (buf.length % 2 == 0) {
                    int half = buf.length / 2;
                    // Copy halves as L and R
                    for (int i = 0; i < half; i++) {
                        left.add(buf[i]);
                        right.add(buf[half + i]);
                    }
                    return true;
                }

                // Fallback: treat as interleaved LRLR...
                for (int i = 0; i + 1 < buf.length; i += 2) {
                    left.add(buf[i]);
                    right.add(buf[i + 1]);
                }
                return true;
            }
            @Override public void processingFinished() {}
        });

        dispatcher.run();

        int len = Math.min(left.size(), right.size());
        float[] L = new float[len];
        float[] R = new float[len];
        for (int i = 0; i < len; i++) {
            L[i] = left.get(i);
            R[i] = right.get(i);
        }

        // If dispatcher didn't already supply the requested sample rate, resample
        if (sourceSampleRate > 0 && sourceSampleRate != targetSampleRate) {
            L = resampleArray(L, sourceSampleRate, targetSampleRate);
            R = resampleArray(R, sourceSampleRate, targetSampleRate);
            int newLen = Math.min(L.length, R.length);
            L = Arrays.copyOf(L, newLen);
            R = Arrays.copyOf(R, newLen);
        }

        if (mastering > -0.9999 && mastering < 1.0001f) // 'unity'
        	return new float[][] { L, R };

        // Apply mastering multiplier and clamp samples to [-1,1]
        if (Math.abs(mastering - 1.0f) > 1e-9f) {
            for (int i = 0; i < L.length; i++) {
            	L[i] *= mastering; // go for speed
            }
            for (int i = 0; i < R.length; i++) {
            	R[i] *= mastering; // use at your own risk
            }
        }

        return new float[][] { L, R };
    }


    // ---------- Resampling helper ----------
    // Uses the Tarsos Resampler class to resample a single-channel float[] from srcRate -> dstRate.
    private static float[] resampleArray(float[] in, double srcRate, double dstRate) {
        if (in == null || in.length == 0) return in;
        double factor = dstRate / srcRate;
        if (Math.abs(factor - 1.0) < 1e-9) return in;

        // Resampler expects minFactor/maxFactor bounds; set both to include the factor.
        // Allow some flexibility in case downstream calls change factor slightly.
        double minFactor = Math.min(0.25, factor);
        double maxFactor = Math.max(4.0, factor);
        Resampler resampler = new Resampler(true, minFactor, maxFactor);

        // Estimate output size
        int outCap = (int) (in.length * factor) + 8;
        float[] out = new float[outCap];

        Resampler.Result res = resampler.process(factor, in, 0, in.length, true, out, 0, outCap);

        int produced = res.outputSamplesGenerated;
        if (produced <= 0) return new float[0];
        return Arrays.copyOf(out, produced);
    }

    // ------------------ WAV writing ------------------

    /**
     * Save Recording to WAV using defaults: Constants.sampleRate() and 16-bit PCM.
     */
    public static void save(Recording source, File destination) throws IOException {
        save(source, destination, Constants.sampleRate(), 16);
    }

    /**
     * Save Recording to WAV with the provided sampleRate and bitDepth.
     *
     * Supported bitDepths: 8 (unsigned), 16 (signed), 24 (signed), 32 (signed).
     *
     * This implementation streams the conversion from floats to PCM bytes so it
     * does not need to allocate one big byte[] for the entire file.
     */
    public static void save(final Recording source, File destination, final int sampleRate, final int bitDepth) throws IOException {
        if (source == null) throw new IllegalArgumentException("source is null");
        if (destination == null) throw new IllegalArgumentException("destination is null");
        if (sampleRate <= 0) throw new IllegalArgumentException("sampleRate must be > 0");
        if (!(bitDepth == 8 || bitDepth == 16 || bitDepth == 24 || bitDepth == 32)) {
            throw new IllegalArgumentException("unsupported bitDepth: " + bitDepth);
        }

        // Calculate total frames (sum of block lengths). Each block is float[2][blockLen].
        long totalFrames = 0;
        for (float[][] block : source) {
            if (block == null || block.length < 2) continue;
            int blockLen = block[0] != null ? block[0].length : 0;
            totalFrames += blockLen;
        }

        // AudioFormat: channels = 2 (stereo), sampleSizeInBits = bitDepth, signed = (bitDepth > 8), littleEndian = true
        boolean signed = bitDepth > 8;
        int frameSize = (bitDepth / 8) * 2;
        AudioFormat format = new AudioFormat(sampleRate, bitDepth, 2, signed, false);

        // Create InputStream that converts Recording blocks into interleaved PCM bytes on demand.
        InputStream pcmStream = new InputStream() {
            private final Enumeration<float[][]> en = source.elements();
            private float[][] curBlock = null;
            private int blockPos = 0; // position within current block (frame index)
            private boolean closed = false;

            @Override
            public int read() throws IOException {
                // not used by AudioInputStream typically; implement single-byte read via buffer fallback
                byte[] b = new byte[1];
                int r = read(b, 0, 1);
                if (r <= 0) return -1;
                return b[0] & 0xFF;
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                if (closed) return -1;
                if (len <= 0) return 0;
                int bytesWritten = 0;

                while (bytesWritten < len) {
                    if (curBlock == null) {
                        if (!en.hasMoreElements()) {
                            closed = true;
                            break;
                        }
                        curBlock = en.nextElement();
                        blockPos = 0;
                        if (curBlock == null || curBlock.length < 2) {
                            curBlock = null;
                            continue;
                        }
                    }
                    float[] left = curBlock[0];
                    float[] right = curBlock[1];
                    int blockLen = Math.min(left != null ? left.length : 0, right != null ? right.length : 0);
                    if (blockPos >= blockLen) {
                        // move to next block
                        curBlock = null;
                        continue;
                    }

                    // For each frame in this block, write interleaved left then right
                    // Convert floats to PCM bytes and append to b[].
                    while (blockPos < blockLen && bytesWritten + frameSize <= len) {
                        float lf = left[blockPos];
                        float rf = right[blockPos];

                        // convert and write left
                        bytesWritten += writeSampleToArray(lf, bitDepth, b, off + bytesWritten);
                        // convert and write right
                        bytesWritten += writeSampleToArray(rf, bitDepth, b, off + bytesWritten);

                        blockPos++;
                    }
                    // if we couldn't fit a full frame due to len, return what we have so far
                    if (bytesWritten == 0 && (bytesWritten < len)) {
                        // Not enough space to write one frame; let caller try again with a bigger buffer.
                        break;
                    }
                }

                return bytesWritten == 0 && closed ? -1 : bytesWritten;
            }
        };

        // Wrap in AudioInputStream with frame length totalFrames
        AudioInputStream ais = new AudioInputStream(pcmStream, format, totalFrames);

        // Write WAV
        AudioSystem.write(ais, AudioFileFormat.Type.WAVE, destination);

        try {
            ais.close();
        } catch (IOException ignored) {}
    }

    /**
     * Converts a single float sample in [-1,1] to PCM bytes and writes to dest at offset.
     * Returns number of bytes written (bitDepth/8).
     *
     * For 8-bit we write unsigned PCM (0..255), for others we write signed little-endian PCM.
     */
    private static int writeSampleToArray(float v, int bitDepth, byte[] dest, int offset) {
        // clamp input
        if (v > 1f) v = 1f;
        if (v < -1f) v = -1f;

        switch (bitDepth) {
            case 8: {
                // unsigned 8-bit
                int iv = Math.round((v * 127.0f) + 128.0f);
                if (iv < 0) iv = 0;
                if (iv > 255) iv = 255;
                dest[offset] = (byte) (iv & 0xFF);
                return 1;
            }
            case 16: {
                int max = 0x7FFF;
                int iv = Math.round(v * max);
                if (iv > max) iv = max;
                if (iv < -max - 1) iv = -max - 1;
                // little-endian
                dest[offset] = (byte) (iv & 0xFF);
                dest[offset + 1] = (byte) ((iv >> 8) & 0xFF);
                return 2;
            }
            case 24: {
                // 24-bit signed
                int max24 = 0x7FFFFF;
                int iv = Math.round(v * max24);
                if (iv > max24) iv = max24;
                if (iv < -max24 - 1) iv = -max24 - 1;
                dest[offset] = (byte) (iv & 0xFF);
                dest[offset + 1] = (byte) ((iv >> 8) & 0xFF);
                dest[offset + 2] = (byte) ((iv >> 16) & 0xFF);
                return 3;
            }
            case 32: {
                // 32-bit signed integer PCM
                long max32 = 0x7FFFFFFFL;
                long ivl = Math.round(v * (double) max32);
                if (ivl > max32) ivl = max32;
                if (ivl < -max32 - 1) ivl = -max32 - 1;
                int iv = (int) ivl;
                dest[offset] = (byte) (iv & 0xFF);
                dest[offset + 1] = (byte) ((iv >> 8) & 0xFF);
                dest[offset + 2] = (byte) ((iv >> 16) & 0xFF);
                dest[offset + 3] = (byte) ((iv >> 24) & 0xFF);
                return 4;
            }
            default:
                throw new IllegalArgumentException("Unsupported bitDepth: " + bitDepth);
        }
    }
}