// java
package net.judah.bridge;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

import judahzone.util.RTLogger;
import judahzone.util.WavConstants;

public class JavaxAudioSink implements AudioOutput, AutoCloseable {
    private final AudioFormat format;
    private SourceDataLine line;
    private final int frameBytes;

    public JavaxAudioSink() {
        format = new AudioFormat(WavConstants.S_RATE, WavConstants.VALID_BITS,
                                 WavConstants.STEREO, true, false);
        frameBytes = (WavConstants.SAMPLE_BYTES) * WavConstants.STEREO;
    }

    private void ensureLineOpen(int jack) {
        if (line != null && line.isOpen()) return;
        try {
            int bufferBytes = jack * frameBytes;
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format, bufferBytes * 4);
            line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(format, Math.max(bufferBytes, (line != null ? line.getBufferSize() : bufferBytes)));
            line.start();
        } catch (Throwable t) {
            RTLogger.warn(this, t);
        }
    }

    @Override
    public void write(float[] left, float[] right, int nframes) {
        if (left == null || right == null) return;
        ensureLineOpen(Math.max(1, nframes));
        if (line == null) return;

        // convert segment to interleaved PCM16 bytes
        byte[] out = new byte[nframes * frameBytes];
        int j = 0;
        for (int i = 0; i < nframes; i++) {
            short ls = floatToPcm16(left[i]);
            short rs = floatToPcm16(right[i]);
            out[j++] = (byte) (ls & 0xFF);
            out[j++] = (byte) ((ls >> 8) & 0xFF);
            out[j++] = (byte) (rs & 0xFF);
            out[j++] = (byte) ((rs >> 8) & 0xFF);
        }

        int written = 0;
        while (written < out.length) {
            try {
                int w = line.write(out, written, out.length - written);
                if (w <= 0) break;
                written += w;
            } catch (Throwable t) {
                RTLogger.warn(this, t);
                break;
            }
        }
    }

    private static short floatToPcm16(float f) {
        if (f > 1f) f = 1f;
        if (f < -1f) f = -1f;
        return (short) Math.round(f * Short.MAX_VALUE);
    }

    @Override public void close() {
        try {
            if (line != null) {
                try { line.flush(); } catch (Throwable ignored) {}
                try { line.stop(); } catch (Throwable ignored) {}
                try { line.close(); } catch (Throwable ignored) {}
            }
        } finally {
            line = null;
        }
    }
}
