package net.judah.omni;

import java.nio.FloatBuffer;
import java.util.Vector;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.Mixer.Info;

public class AudioTools  {

    /** approx. Acoustic impedance of air at room temperature in Pa·s/m */
	public static final float ACOUSTIC_IMPEDANCE = 415;

	public static void silence(FloatBuffer a) {
		a.rewind();
		while(a.hasRemaining())
			a.put(0f);
		a.rewind();
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

	/** MIX in and out */
	public static void mix(FloatBuffer in, FloatBuffer out) {
		in.rewind();
		out.rewind();
		for (int z = 0; z < out.capacity(); z++)
			out.put(out.get(z) + in.get(z));
	}

	public static void mix(FloatBuffer in, float[] out) {
		in.rewind();
		for (int i = 0; i < out.length; i++)
			out[i] += in.get();
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

	public static float[][] copy(float[][] in, float[][] out) {
		for (int i = 0; i < in.length; i++) {
			for (int j = 0; j < out[i].length; j++) {
				out[i][j] = in[i][j];
			}
		}
		return out;
	}

	/**@param sr sampleRate  in Hz
	 * @return the frequency above which aliasing artifacts are found */
	public static float nyquistFreq(float sampleRate) {
		return sampleRate / 2f;
	}

	/**
	 * Calculates and returns the root mean square of the signal. Please
	 * cache the result since it is calculated every time.
	 * @param buffer The audio buffer to calculate the RMS for.
	 * @return The <a
	 *         href="http://en.wikipedia.org/wiki/Root_mean_square">RMS</a> of
	 *         the signal present in the current buffer.
	 */
	public static double rms(float[] buffer) {
		double result = 0.0;
		for (int i = 0; i < buffer.length; i++)
			result += buffer[i] * buffer[i];

		result = result / Double.valueOf(buffer.length);
		return Math.sqrt(result);
	}

	public static double intensity(float[] buffer) {
		double rms = rms(buffer);
		return (rms * rms) / ACOUSTIC_IMPEDANCE;
	}

	/** @return seconds.## */
	public static String toSeconds(long millis) {
		return new StringBuffer().append( millis / 1000).append(".").append(
				(millis / 10) % 100).append("s").toString();
	}

	public static HiLo avg(float[] in) {
	    float sumPositive = 0;
	    float sumNegative = 0;
	    int countPositive = 0;
	    int countNegative = 0;
	    for (float val : in) {
	        if (val > 0) {
	            sumPositive += val;
	            countPositive++;
	        } else if (val < 0) {
	            sumNegative += val;
	            countNegative++;
	        }
	    }
	    float avgPositive = countPositive > 0 ? sumPositive / countPositive : 0;
	    float avgNegative = countNegative > 0 ? sumNegative / countNegative : 0;
	    return new HiLo(avgPositive, avgNegative);
	}

	public static HiLo peaks(float[] in) {
        float min = Float.MAX_VALUE;
        float max = Float.MIN_VALUE;
        for (float val : in) {
        	if (val < min) {
        		min = val;
        	}
        	if (val > max)
        		max = val;
        }
        return new HiLo(max, min);
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

}

