package net.judah.gui.fft_temp;

public class SineWaveGenerator {


    public static double[] generateSineWave(double amplitude, double frequency, double samplingRate, int numSamples) {
        double[] sineWave = new double[numSamples];
        double angularFrequency = 2 * Math.PI * frequency / samplingRate;

        for (int i = 0; i < numSamples; i++) {
            sineWave[i] = amplitude * Math.sin(angularFrequency * i);
        }

        return sineWave;
    }

    public static void test() {
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

    /**
	 * Fill `buf` with a sine wave at freqHz. Returns the updated phase (radians) to use
	 * for the next call so the tone is continuous across buffers.
	 *
	 * @param buf       float[] buffer to fill (length = FFT_SIZE)
	 * @param freqHz    desired frequency in Hz
	 * @param sampleRate sample rate in Hz (e.g. 48000.0)
	 * @param amplitude amplitude (0..1)
	 * @return new phase in radians to pass to the next call
	 */
	public static double fillSine(float[] buf, double freqHz, double sampleRate, double amplitude) {
		double phase = 0;
		if (buf == null || buf.length == 0) return phase;
	    final double twoPi = 2.0 * Math.PI;
	    // phase increment per sample (radians)
	    final double phaseInc = twoPi * freqHz / sampleRate;

	    // keep amplitude safe
	    final double a = Math.max(0.0, Math.min(1.0, amplitude));

	    for (int i = 0; i < buf.length; i++) {
	        buf[i] = (float) (a * Math.sin(phase));
	        phase += phaseInc;
	        // wrap phase into [0, 2PI) to avoid unbounded growth
	        if (phase >= twoPi) phase -= twoPi * Math.floor(phase / twoPi);
	    }
	    return phase;
	}

}

