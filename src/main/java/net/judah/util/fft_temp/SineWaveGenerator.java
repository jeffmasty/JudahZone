package net.judah.util.fft_temp;

public class SineWaveGenerator {





    public static double[] generateSineWave(double amplitude, double frequency, double samplingRate, int numSamples) {
        double[] sineWave = new double[numSamples];
        double angularFrequency = 2 * Math.PI * frequency / samplingRate;

        for (int i = 0; i < numSamples; i++) {
            sineWave[i] = amplitude * Math.sin(angularFrequency * i);
        }

        return sineWave;
    }

    public static void main(String[] args) {
        double amplitude = 1.0; // Amplitude of the sine wave
        double frequency = 220.0; // Frequency of the sine wave in Hz
        double samplingRate = 48000.0; // Sampling rate in Hz
        int numSamples = 1024; // Number of samples
        double[] sineWave = generateSineWave(amplitude, frequency, samplingRate, numSamples);

        // Print the generated sine wave samples
        for (int i = 0; i < numSamples; i++) {
            System.out.println("Sample " + i + ": " + sineWave[i]);
        }
    }
}