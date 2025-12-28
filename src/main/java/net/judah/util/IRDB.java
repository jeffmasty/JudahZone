package net.judah.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sound.sampled.UnsupportedAudioFileException;

import be.tarsos.dsp.util.fft.FFT;

/**scan a folder (Folders.getIR()), prepare and cache FFT spectra for each WAV.
 * Exceptions swallowed/logged per file.
 * A Nice Collection of IRs:  https://github.com/orodamaral/Speaker-Cabinets-IRs/
 */
public final class IRDB {

    public static record IrEntry(String name, float[] irFreq) {
        @Override public String toString() { return name; }
    }

    private static final int FFT_SIZE = Constants.fftSize();
    private static final int IR_SIZE  = FFT_SIZE / 2;
    public static final float MASTERING = 0.3f; // cut IR volume

    private static final FFT FFT_FOR_IR = new FFT(FFT_SIZE);

    private final List<IrEntry> entries = new ArrayList<>();

    public IRDB(File folder) {
        // Tarsos too verbose
        Logger.getLogger("be.tarsos.dsp.io.PipeDecoder").setLevel(Level.SEVERE);
    	load(Folders.getIR());
    }

    private void load(File folder) {

        File[] files = Folders.sort(folder);
        if (files == null || files.length == 0) {
            RTLogger.warn(IRDB.class, "No IR files found in " + Folders.getIR());
            return;
        }

        Map<String, float[]> nameToFreq = new LinkedHashMap<>();
        for (File f : files) {
            if (!f.getName().toLowerCase().endsWith(".wav")) continue;
            try {
                final float[] irTime = WavFile.getMono(f, Constants.fftSize() / 2);
                if (irTime == null || irTime.length == 0) return;

                // loudness, front and tail windows // TODO toggle?
                mastering(irTime, 32, irTime.length);

                float[] irFreq = prepareIrSpectrum(irTime);
                String name = f.getName().replaceFirst("(?i)\\.wav$", "");
                nameToFreq.put(name, irFreq);
                RTLogger.debug(IRDB.class, "Loaded IR: " + name + " (" + irTime.length + " samples)");
            } catch (IOException | UnsupportedAudioFileException e) {
                RTLogger.warn(IRDB.class, "Failed to load IR " + f.getName() + ": " + e);
            } catch (Throwable t) {
                RTLogger.warn(IRDB.class, "Unexpected error while loading " + f.getName() + ": " + t);
            }
        }

        for (Map.Entry<String, float[]> e : nameToFreq.entrySet()) {
            entries.add(new IrEntry(e.getKey(), e.getValue()));
        }

        if (entries.isEmpty()) {
            RTLogger.warn(IRDB.class, "IR DB empty after scan.");
        }
    }

    private static float[] prepareIrSpectrum(float[] irTime) {
        float[] fftInOut = new float[FFT_SIZE * 2];
        Arrays.fill(fftInOut, 0f);

        int copyLen = Math.min(irTime.length, IR_SIZE);
        System.arraycopy(irTime, 0, fftInOut, 0, copyLen);

        FFT_FOR_IR.forwardTransform(fftInOut); // in-place realForward -> complex

        float[] irFreq = new float[FFT_SIZE * 2];
        System.arraycopy(fftInOut, 0, irFreq, 0, irFreq.length);
        return irFreq;
    }

    // Standard gain, window helps with dry/wet phase cancellation
    private static void mastering(float[] irTime, int N, int len) {
        // Mastering
        for (int i = 0; i < len; i++)
            irTime[i] = irTime[i] * MASTERING;

        N = Math.max(1, Math.min(N, irTime.length));
        for (int i = 0; i < N; i++) {
            // half-Hann: 0.5 * (1 - cos(pi * i / (N - 1))) goes from 0..1
            double w = 0.5 * (1.0 - Math.cos(Math.PI * i / (N - 1)));
            irTime[i] *= (float) w;
        }

        // tail
        for (int i = 100; i <= 0; i++)
        	irTime[len - i] *= i * 0.01f;

    }


    public int size() { return entries.size(); }
    public List<IrEntry> getEntries() { return entries; }
    public String[] getNames() {
        String[] n = new String[entries.size()];
        for (int i = 0; i < n.length; i++) n[i] = entries.get(i).name;
        return n;
    }
    public IrEntry get(int idx) { return entries.get(idx); }
}