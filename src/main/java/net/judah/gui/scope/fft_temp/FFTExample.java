package net.judah.gui.scope.fft_temp;

import be.tarsos.dsp.util.fft.FFT;
import be.tarsos.dsp.util.fft.HammingWindow;
import net.judah.util.Constants;

public class FFTExample {
    public static void main(String[] args) {
        final float frequency = 10000; // Frequency of the sine wave in Hz
        final int sampleRate = Constants.sampleRate();
        final int size = Constants.bufSize();
        float[] sin = new float[size];
        // Fill audioData with a 440 Hz sine wave
        for (int i = 0; i < size; i++)
            sin[i] = (float) Math.sin(Math.TAU * frequency * i / sampleRate);
        for (int i = 0; i < size; i++) {
            sin[i] = (float) Math.sin(2 * Math.PI * frequency * i / sampleRate);
        }
        float[] fftData = new float[size * 2];
        for (int i = 0; i < size; i++)
        	fftData[i*2] = sin[i];
        FFT mine = new FFT(size * 2, new HammingWindow());
        mine.forwardTransform(fftData);

        int maxIdx = -1;
        float max = 0;

        // Print the FFT result
        for (int i = 0; i < size; i++) {
            float real = fftData[i * 2];
            float imaginary = fftData[i * 2 + 1];
            float magnitude = (float) Math.sqrt(real * real + imaginary * imaginary);

            if (magnitude > max) {
                max = magnitude;
                maxIdx = i;
            }

            System.out.printf("Frequency bin %d: %f Hz, Real part = %f, Imaginary part = %f%n",
                    i, mine.binToHz(i, sampleRate), real, imaginary);
        }
        System.out.println("maxMag: " + max + " at " + maxIdx / 2 + " hz: " + mine.binToHz(maxIdx/2, sampleRate));


    }
}