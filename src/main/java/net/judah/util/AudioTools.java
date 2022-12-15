package net.judah.util;

import java.nio.FloatBuffer;

public class AudioTools  {

	private static int z;
	private static float[] workArea = new float[Constants.bufSize()];

	public static void silence(FloatBuffer a) {
		a.rewind();
		while(a.hasRemaining())
			a.put(0f);
		a.rewind();
	}
	public static void silence(FloatBuffer[] bufs) {
		for (FloatBuffer buf : bufs)
			silence(buf);
	}
	public static void silence(float[][] buf) {
		for (float[] ch : buf) 
			silence(ch);
	}

	public static void silence(float[] mono) {
		for (int i = 0; i < mono.length; i++)
			mono[i] = 0f;
	}

	/** MIX
	 * @param overdub
	 * @param oldLoop*/
	public static float[][] overdub(float[][] overdub, float[][] oldLoop) {
		float[][] channels = new float[oldLoop.length][];
		float[] in, out, result;
		for (int channel = 0; channel < oldLoop.length; channel++) {
			in = overdub[channel];
			out = oldLoop[channel];
			result = new float[out.length];
			for (int x = 0; x < out.length; x++) {
				result[x] = in[x] + out[x];
			}
			channels[channel] = result;
		}
		return channels;
	}
	
	public static void mix(FloatBuffer[] in, FloatBuffer[] out, int pan) {
	
	}


	/** MIX in and out with gain applied to the input*/
	public static void mix(FloatBuffer in, FloatBuffer out) {
		in.rewind();
		out.rewind();
		for (int z = 0; z < out.capacity(); z++)
			out.put(out.get(z) + in.get(z));
	}

	/** MIX in and out with gain applied to the input*/
	public static void mix(FloatBuffer in, float gain, FloatBuffer out) {
		if (1f == gain) {
			mix(in, out);
			return;
		}
		in.rewind();
		out.rewind();
		for (int z = 0; z < out.capacity(); z++)
			out.put(out.get(z) + gain * in.get(z));
	}

	/** process replace */
	public static void processGain(float[] in, float[] out, float vol) {
	     for (z = 0; z < Constants.bufSize(); z++)
	         out[z] = in[z] * vol;
	}

	/** MIX */
	public static void processAdd(FloatBuffer in, float[] out) {
		in.rewind();
		for (int i = 0; i < out.length; i++)
			out[i] += in.get();
	}

	/** MIX */
	public static void processAddGain(float factor, FloatBuffer in, float[] out) {
		in.rewind();
		for (int i = 0; i < out.length; i++)
			out[i] += in.get() * factor;
	}

	public static float abs2(FloatBuffer buf) {
		float result = Float.MIN_VALUE;
		buf.rewind();
		for(int i = 0; i < Constants.bufSize(); i++) {
			float f = Math.abs(buf.get());
			if (f > result)
				result = f;
		}
		return result;
	}
	
	public static void mix(float[] in, FloatBuffer out) {
		out.rewind();
		out.get(workArea);
		out.rewind();
		for (z = 0; z < Constants.bufSize(); z++)
			out.put(workArea[z] + in[z]);
	}
	
	public static void mix(float[] in, FloatBuffer out, float inGain) {
		out.get(workArea);
		out.rewind();
		for (z = 0; z < Constants.bufSize(); z++)
			out.put(workArea[z] + in[z] * inGain);
	}
	
	
	public static void processGain(FloatBuffer buffer, float gain) {
		buffer.rewind();
		for (z = 0; z < Constants.bufSize(); z++) {
			buffer.put(buffer.get(z) * gain);
		}
	}
	
	public static float[][] copy(float[][] in, float[][] out) {
		for (int i = 0; i < in.length; i++) {
			for (int j = 0; j < out[i].length; j++) {
				out[i][j] = in[i][j];
			}
		}
		return out;
	}
	public static float[] copy(FloatBuffer input) {
		input.rewind();
		float[] result = new float[input.capacity()];
		replace(1f, input, result);
		return result;
	}
	
	public static void replace(float gain, FloatBuffer in, float[] out) {
		in.rewind();
		for (int i = 0 ; i < out.length; i++) 
			out[i] = in.get() * gain;
	}
}

