package net.judah.util;

import java.nio.FloatBuffer;

import net.judah.looper.Recording;

public class AudioTools  {

	public static void silence(FloatBuffer a) {
		a.rewind();
		while(a.hasRemaining())
			a.put(0f);
		a.rewind();
	}
	
	public static void silence(Recording recording) {
		for (int frame = 0; frame < recording.size(); frame++) 
			for (int channel = 0; channel < recording.get(frame).length; channel++) 
				zero(recording.get(frame)[channel]);
	}

	public static void silence(Recording recording, Integer end) {
		if (end == null || end == 0)
			return;
		if (end > recording.size())
			end = recording.size();
		for (int frame = 0; frame < end; frame++) 
			for (int ch = Constants.LEFT; ch < recording.get(frame).length; ch++) 
				zero(recording.get(frame)[ch]);
	}
	
	private static void zero(float[] channel) {
		for (int i = 0; i < channel.length; i++)
			channel[i] = 0;
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

	public static void replace(float gain, FloatBuffer in, float[] out) {
		in.rewind();
		for (int i = 0 ; i < out.length; i++) 
			out[i] = in.get() * gain;
	}

	public static void replace(float[] in, FloatBuffer out, float gain) {
		out.rewind();
		for (int i = 0; i < in.length; i++)
			out.put(in[i] * gain);
	}

	public static void gain(FloatBuffer buffer, float gain) {
		buffer.rewind();
		for (int z = 0; z < Constants.bufSize(); z++) {
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

	public static void copy(FloatBuffer in, int bufSize, float[] result) {
		in.rewind();
		for (int i = 0 ; i < bufSize; i++) 
			result[i] = in.get();
		in.rewind();
	}

	public static void silence(float[][] stereo) {
		for (int ch = 0; ch < stereo.length; ch++)
			for (int i = 0; i < stereo[ch].length; i++)
				stereo[ch][i] = 0;
	}
	
}

