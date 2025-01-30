package net.judah.util.fft_temp;

import be.tarsos.dsp.util.fft.FFT;
import be.tarsos.dsp.util.fft.HammingWindow;
import net.judah.util.Constants;

public class FFTExample {
    public static void main(String[] args) {
        final float frequency = 440.0f; // Frequency of the sine wave in Hz
        final int sampleRate = Constants.sampleRate();
        final int size = Constants.bufSize();

        float[] audioData = new float[size * 2];

        // Fill audioData with a 440 Hz sine wave
        for (int i = 0; i < size; i++) {
            audioData[i * 2] = (float) Math.sin(2.0 * Math.PI * frequency * i / sampleRate);
        }

        // Create FFT object and perform FFT
        new FFT(audioData.length, new HammingWindow()).forwardTransform(audioData);

        // Print the FFT result
        for (int i = 0; i < audioData.length; i += 2) {
            float real = audioData[i];
            float imaginary = audioData[i + 1];
            System.out.printf("Frequency bin %d: Real part = %f, Imaginary part = %f%n", i / 2, real, imaginary);
        }
    }
}