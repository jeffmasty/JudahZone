package net.judah.gui.fft_temp;

import java.security.InvalidParameterException;

import be.tarsos.dsp.util.fft.FFT;
import be.tarsos.dsp.util.fft.HammingWindow;

public class FFTTest {

	public static void error() {
		final int FFT_SIZE = 4096; // any power of 2 should work
	    final FFT noWindow = new FFT(FFT_SIZE); // pass
	    final FFT withWindow = new FFT(FFT_SIZE, new HammingWindow()); // cause of later error

	    final float[] test1 = new float[FFT_SIZE * 2];
	    final float[] test2 = new float[FFT_SIZE * 2];
	    float[] magnitudes; // FFT_SIZE / 2
	    float max; // test var

		// generate a 440 Hz tone at amplitude 0.6
		final float[] sinWave = new float[FFT_SIZE];
		final double S_RATE = 48000.0; // any should work
	    final double TWO_PI = 2.0 * Math.PI;
		final double hz = 440.0;
	    final double amplitude = 0.6;
	    final double step = TWO_PI * hz / S_RATE;
		double phase = 0;
	    for (int i = 0; i < sinWave.length; i++) {
	        sinWave[i] = (float) (amplitude * Math.sin(phase));
	        phase += step;
	        if (phase >= TWO_PI) // clamp
	        	phase -= TWO_PI * Math.floor(phase / TWO_PI);
	    }

	    // copy audio into first half of test arrays  (zeros are in last half)
		System.arraycopy(sinWave, 0, test1, 0, FFT_SIZE);
		System.arraycopy(sinWave, 0, test2, 0, FFT_SIZE);

		max = 0;
		noWindow.forwardTransform(test1);
		magnitudes = new float[FFT_SIZE / 2];
		noWindow.modulus(test1, magnitudes);
		for (int i = 0 ; i < magnitudes.length; i++) {
		    float m = magnitudes[i];
		    if (!Float.isFinite(m) || m < 0f)
		    	throw new InvalidParameterException(i + ": " + m);
		    if (m > max)
		    	max = m;
		}
		assert max > 0;
		System.out.println("no-window transform passed: " + max);

		max = 0;
		withWindow.forwardTransform(test2); // <-- window causes error
		magnitudes = new float[FFT_SIZE / 2];
		withWindow.modulus(test2, magnitudes);
		for (int i = 0 ; i < magnitudes.length; i++) {
		    float m = magnitudes[i];
		    if (!Float.isFinite(m) || m < 0f)
		    	throw new InvalidParameterException(i + ": " + m);
		    if (m > max)
		    	max = m;
		}
		assert max > 0;
		System.out.println("window transform passed: " + max);
	}
	// test various FFT libraries by running them against files many times and timing the results.

	// RUN: forward transform + magnitudes (modulus)

	/* equivalent:
	 *
		System.arraycopy(buffer.getLeft(), 0, transformBuffer, 0, FFT_SIZE); // mono
		fft.forwardTransform(transformBuffer);
		float[] amplitudes = new float[AMPLITUDES];
		fft.modulus(transformBuffer, amplitudes);

	 */


	/*
// JTransforms fields (add imports: org.jtransforms.fft.DoubleFFT_1D)
private final org.jtransforms.fft.DoubleFFT_1D jfft = new org.jtransforms.fft.DoubleFFT_1D(FFT_SIZE);
private final double[] jtInterleaved = new double[FFT_SIZE * 2]; // re,im,re,im,...
private final float[] jtAmplitudes = new float[AMPLITUDES];     // reusable output
private final boolean jtNormalize = false; // set true if you want amplitudes /= FFT_SIZE

*/

/*
	// copy mono samples into interleaved double buffer (real,imag interleaved)
	float[] left = buffer.getLeft(); // length = FFT_SIZE
	for (int i = 0, j = 0; i < FFT_SIZE; i++, j += 2) {
	    jtInterleaved[j] = left[i];
	    jtInterleaved[j + 1] = 0.0d;
	}

	// forward in-place complex FFT
	jfft.complexForward(jtInterleaved);

	// compute half-spectrum magnitudes (bins 0 .. FFT_SIZE/2 - 1)
	for (int k = 0, j = 0; k < AMPLITUDES; k++, j += 2) {
	    double re = jtInterleaved[j];
	    double im = jtInterleaved[j + 1];
	    double mag = Math.hypot(re, im); // stable hypot
	    if (jtNormalize) mag /= (double) FFT_SIZE;
	    jtAmplitudes[k] = (float) mag;
	}

	// jtAmplitudes now contains the amplitudes (use it instead of allocating a new array)
	float[] amplitudes = jtAmplitudes;
*/
/**
 // fields (create once)
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;
import org.apache.commons.math3.complex.Complex;

private final FastFourierTransformer acmFft =
    new FastFourierTransformer(DftNormalization.STANDARD);

// per-frame: equivalent to your Tarsos code
double[] real = new double[FFT_SIZE];
float[] left = buffer.getLeft(); // float[] from your Recording
for (int i = 0; i < FFT_SIZE; i++) {
    real[i] = left[i]; // copy and convert to double
}

// forward transform -> Complex[] (length == FFT_SIZE)
Complex[] spec = acmFft.transform(real, TransformType.FORWARD);

// extract half-spectrum magnitudes (0 .. FFT_SIZE/2-1)
float[] amplitudes = new float[AMPLITUDES];
for (int k = 0; k < AMPLITUDES; k++) {
    amplitudes[k] = (float) spec[k].abs();
}

// Note: Apache Commons Math uses its own normalization mode (STANDARD/UNITARY).
// If your downstream expects a different scaling, adjust by dividing/multiplying
// by FFT_SIZE or sqrt(FFT_SIZE) accordingly.
JTransforms (recommended for performance)
Operates in-place on a double[] interleaved [re,im,re,im,...] buffer
Avoids Complex allocations and is very fast; use DoubleFFT_1D once and reuse
Matches your existing transformBuffer layout if you use TRANSFORM = FFT_SIZE*2
Java
// fields (create once)
import org.jtransforms.fft.DoubleFFT_1D;

private final DoubleFFT_1D jfft = new DoubleFFT_1D(FFT_SIZE);

// per-frame: high-performance path
float[] left = buffer.getLeft(); // float[] length FFT_SIZE

// reuse a double[] interleaved buffer (allocate once as a field for best perf)
double[] interleaved = new double[FFT_SIZE * 2]; // re,im, re,im, ...
// copy real samples into interleaved real slots, zero imag
for (int i = 0, j = 0; i < FFT_SIZE; i++, j += 2) {
    interleaved[j] = left[i];
    interleaved[j + 1] = 0.0;
}

// compute complex forward in-place
jfft.complexForward(interleaved);

// compute half-spectrum magnitudes (bins 0 .. FFT_SIZE/2-1)
float[] amplitudes = new float[AMPLITUDES];
for (int k = 0; k < AMPLITUDES; k++) {
    int j = k * 2;
    double re = interleaved[j];
    double im = interleaved[j + 1];
    amplitudes[k] = (float) Math.hypot(re, im); // stable hypot(re,im)
}

// Note: JTransforms does not scale the forward transform. If you need
// the same amplitude scaling as another library, divide amplitudes by FFT_SIZE.*/

}
