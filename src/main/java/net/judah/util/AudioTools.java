package net.judah.util;

import java.nio.FloatBuffer;
import java.util.Vector;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.Mixer.Info;

public class AudioTools  {

	public static void silence(FloatBuffer a) {
        a.rewind();
        int limit = a.limit();
        for (int i = 0; i < limit; i++)
            a.put(0f);
	}

	/** MIX in and out */
	public static void mix(FloatBuffer in, float gain, FloatBuffer out) {
		out.rewind();
		in.rewind();
	    int capacity = out.capacity();
	    for (int i = 0; i < capacity; i++)
	        out.put(i, out.get() + in.get() * gain);
	}

	/** MIX in and out */
	public static void mix(FloatBuffer in, FloatBuffer out) {
		out.rewind();
		in.rewind();
	    int capacity = out.capacity();
	    for (int i = 0; i < capacity; i++)
	        out.put(i, out.get() + in.get());
	}

	public static void mix(float[] in, float gain, FloatBuffer out) {
		out.rewind();
		int capacity = out.capacity();
		for (int i = 0; i < capacity; i++)
			out.put(i, out.get() + in[i] * gain);
	}

	public static void mix(FloatBuffer in, float[] out) {
		in.rewind();
		for (int i = 0; i < out.length; i++)
			out[i] += in.get();
	}

	/** MIX
	 * @param overdub
	 * @param oldLoop*/
	public static float[][] overdub(float[][] overdub, float[][] oldLoop) {
		float[] in, out;//, result;
		for (int channel = 0; channel < oldLoop.length; channel++) {
			in = overdub[channel];
			out = oldLoop[channel];
			for (int x = 0; x < out.length; x++)
				out[x] = in[x] + out[x];
		}
		return oldLoop;
	}

	/** MIX */
	public static void add(float factor, FloatBuffer in, float[] out) {
		in.rewind();
		for (int i = 0; i < out.length; i++)
			out[i] += in.get() * factor;
	}

	public static void replace(float[] in, FloatBuffer out, float gain) {
		out.rewind();
		for (int i = 0; i < in.length; i++)
			out.put(in[i] * gain);
	}

	public static void replace(float[] in, FloatBuffer out) {
		out.rewind();
		for (int i = 0; i < out.capacity(); i++)
			out.put(in[i]);
	}

	public static void gain(FloatBuffer buffer, float gain) {
		buffer.rewind();
		for (int z = 0; z < buffer.capacity(); z++) {
			buffer.put(buffer.get(z) * gain);
		}
	}

	public static void copy(FloatBuffer in, FloatBuffer out) {
		in.rewind();
		out.rewind();
		while(in.hasRemaining() && out.hasRemaining())
			out.put(in.get());
	}

	public static void copy(FloatBuffer in, float[] out) {
		in.rewind();
		int max = in.remaining();
		if (out.length < max)
			max = out.length;
		for (int i = 0; i < max; i++)
			out[i] = in.get();
	}

	public static void copy(float[][] in, float[][] out) {
		for (int i = 0; i < in.length; i++) {
			for (int j = 0; j < out[i].length; j++) {
				out[i][j] = in[i][j];
			}
		}
	}

	/** malloc */
	public static float[][] clone(float[][] in) {
		float[][] out = new float[in.length][in[0].length];
		copy(in, out);
		return out;
	}

	/** Calculates and returns the root mean square of the signal. Please
	 * cache the result since it is calculated every time.
	 * @param buffer The audio buffer to calculate the RMS for.
	 * @return The <a href="http://en.wikipedia.org/wiki/Root_mean_square">RMS</a> of
	 *         the signal present in the current buffer. */
	public static float rms(float[] buffer) {
		float result = 0f;
		for (int i = 0; i < buffer.length; i++)
			result += buffer[i] * buffer[i];

		result = result / buffer.length;
		return (float)Math.sqrt(result);
	}

	/** Source: be.tarsos.dsp.AudioEvent
	 * Converts a linear (rms) to a dB value. */
	public static double linearToDecibel(final double value) {
		return 20.0 * Math.log10(value);
	}

	/**@param sr sampleRate  in Hz
	 * @return the frequency above which aliasing artifacts are found */
	public static float nyquistFreq(float sampleRate) {
		return sampleRate / 2f;
	}
	/** @return nyquist for the system's sample rate */
	public static float nyquistFreq() {
		return nyquistFreq(Constants.sampleRate());
	}

	public static String sampleToSeconds(long sampleNum) {
		return String.format("%.2f", (sampleNum / (Constants.fps() * Constants.bufSize())));
	}

	/** @return seconds.## */
	public static String toSeconds(long millis) {
		return new StringBuffer().append( millis / 1000).append(".").append(
				(millis / 10) % 100).append("s").toString();
	}

	public static Vector<Mixer.Info> getMixerInfo(
			final boolean supportsPlayback, final boolean supportsRecording) {
		final Vector<Mixer.Info> infos = new Vector<Mixer.Info>();
		final Mixer.Info[] mixers = AudioSystem.getMixerInfo();
		for (final Info mixerinfo : mixers) {
			if (supportsRecording
					&& AudioSystem.getMixer(mixerinfo).getTargetLineInfo().length != 0) {
				// Mixer capable of recording audio if target line length != 0
				infos.add(mixerinfo);
			} else if (supportsPlayback
					&& AudioSystem.getMixer(mixerinfo).getSourceLineInfo().length != 0) {
				// Mixer capable of audio play back if source line length != 0
				infos.add(mixerinfo);
			}
		}
		return infos;
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

    /**Estimate an RMS-like amplitude from spectral magnitudes in an absolute bin range. */
    public static double computeFrameRms(float[] amplitudes, int absStartBin, int absEndBin) {
        absStartBin = Math.max(0, Math.min(amplitudes.length - 1, absStartBin));
        absEndBin = Math.max(0, Math.min(amplitudes.length - 1, absEndBin));
        if (absEndBin < absStartBin) return 0.0;

        double sumPower = 0.0;
        int count = 0;
        for (int i = absStartBin; i <= absEndBin; i++) {
            float mag = amplitudes[i];
            if (!Float.isFinite(mag) || mag <= 0f) continue;
            double p = mag * (double) mag;
            sumPower += p;
            count++;
        }
        if (count == 0) return 0.0;
        double meanPower = sumPower / count;
        return Math.sqrt(meanPower); // RMS-like amplitude
    }

}

