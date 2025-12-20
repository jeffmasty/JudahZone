package net.judah.gui.scope.fft_temp;

import be.tarsos.dsp.util.fft.FloatFFT;
import lombok.Setter;

/*
https://www.youtube.com/watch?v=AyUk-bZHJxI

FFT:  Time axis vs. Freq axis
FFT separates a signal into individual sine waves through time at specific frequency peaks.
	called sinusoids. properties of sinusoids --> complex numbers
					real part of sinusoid represents amplitude of the cosine -> W of Triangle
					imaginary part represents amplitude of the sine wave	 -> H of Triangle
					amplitude is + or -
					magnitude is scaler, always positive					 -> L of hypotenuse
																		Phase = Theta of hypotenuse
																		Phase = Tan^-1(	H/W )
																		atan2

For plotting FFT-analyzed frequency data, you would follow a similar approach but label the axes accordingly (e.g., frequency on the x-axis and magnitude on the y-axis).
 */
/*
 * Choose Freq
 * Optimize Phase
 * Calc Magnitude
 */
public class Sinusoid {

	private static final String NL = System.lineSeparator();

	public static final float sampleRate = 48000;

	public final int size;

	private final float[] real; 		// cosine amplitude			W of triangle
	private final float[] imaginary;	// sine amplitude			H of triangle
//	private float[] magnitude; 	// √(H² * W²), 				L of hypotenuse
//	private float[] computed;		// atan2(H, W)				Angle of hypotenuse

	@Setter private float phase;


	public Sinusoid(float[] raw) {
		size = raw.length;
		FloatFFT fft = new FloatFFT(size);
		float[] result = new float[2 * size];
		System.arraycopy(raw, 0, result, 0, size);
		fft.complexForward(result);

		real = new float[size];
		imaginary = new float[size];

		for (int i = 0; i < size; i++) {
			real[i] = result[i * 2];
			imaginary[i] = result[i * 2 + 1];
		}

	}

	// sampleRate and blockSize yields:  duration per block and frequency resolution
		// duration = blockLength / sampleRate
		//					512  /   48k  = 10.67 ms/block
		// deltaFreq = 1  / duration
		// 					1   /	10.67 = 93.75 Hz
		//
		// windowing smoothes leakage
		//
		//

	public float frequency(int idx) {
		return idx / (float) size / sampleRate;
	}

	public double magnitude(int idx) {
		return magnitude(real[idx], imaginary[idx]);
	}

	public double magnitude(float real, float imaginary) {
		return Math.sqrt(real * real + imaginary * imaginary);
	}

	public double phase(float real, float imaginary) {
		return Math.atan2(real, imaginary);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("Sinusoid").append(NL);

		for (int idx = 0; idx < size; idx++) {
			sb.append(idx).append(": ").append(frequency(idx)).append("Hz. R/I: ");
			sb.append(real[idx]).append(" ").append(imaginary[idx]).append(" magnitude: ").append(magnitude(idx));
//			sb.append(" phase: ").append(phase(idx));
			sb.append(NL);
		}
		return sb.toString();
	}

//	String shape; // enum
//	String harmonics;
//	LFO lfo;

    public static float[] generateSineWave(float amplitude, double frequency, double samplingRate, int numSamples) {
        float[] sineWave = new float[numSamples];
        double angularFrequency = 2 * Math.PI * frequency / samplingRate;

        for (int i = 0; i < numSamples; i++) {
            sineWave[i] = amplitude * (float)Math.sin(angularFrequency * i);
        }

        return sineWave;
    }
}

/*
1. Read the audio data: Read the audio file and extract the audio samples for a single buffer.

2. Apply FFT to the audio data: Use a library like Apache Commons Math or JTransforms to perform the FFT on the audio samples to convert them from the time domain to the frequency domain.

3. Calculate the magnitude spectrum: Extract the magnitude spectrum from the complex FFT result to get the amplitude values for different frequencies.

4. Plot the volume at different frequencies: Create a plot or graph using a library like JFreeChart to display the amplitude values at different frequencies for the single sample buffer.

Here's a simplified example using Apache Commons Math library to calculate and plot the volume at different frequencies for a single sample buffer:

```java
import org.apache.commons.math3.transform.*;
import org.apache.commons.math3.complex.*;

// Assuming you have an array of audio samples in 'audioSamples'

// Apply FFT to the audio data
FastFourierTransformer transformer = new FastFourierTransformer(DftNormalization.STANDARD);
Complex[] fftResult = transformer.transform(audioSamples, TransformType.FORWARD);

// Calculate the magnitude spectrum
double[] magnitudeSpectrum = new double[fftResult.length];
for (int i = 0; i < fftResult.length; i++) {
    magnitudeSpectrum[i] = fftResult[i].abs();
}

// Plot the volume at different frequencies
// You can use JFreeChart or any other charting library to create a plot based on 'magnitudeSpectrum'
// Refer to JFreeChart documentation for creating plots

// Example JFreeChart code to create a line chart
XYSeries series = new XYSeries("Frequency Spectrum");
for (int i = 0; i < magnitudeSpectrum.length; i++) {
    series.add(i, magnitudeSpectrum[i]);
}
XYSeriesCollection dataset = new XYSeriesCollection(series);
JFreeChart chart = ChartFactory.createXYLineChart("Frequency Spectrum", "Frequency", "Magnitude", dataset, PlotOrientation.VERTICAL, true, true, false);

// Display the chart
ChartPanel chartPanel = new ChartPanel(chart);
*/