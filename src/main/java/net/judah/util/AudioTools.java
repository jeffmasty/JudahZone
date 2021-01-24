package net.judah.util;

import java.nio.FloatBuffer;

public class AudioTools  {

	private static int z;
	private static float[] workArea = new float[Constants.bufSize()];

	public static void processSilence(FloatBuffer a) {
		a.rewind();
		while(a.hasRemaining())
			a.put(0f);
		a.rewind();
	}
	public static void processSilence(float[][] buf) {
		for (float[] ch : buf) {
			for (int i = 0; i < ch.length; i++)
				ch[i] = 0f;
		}
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

	/** MIX in and out with gain applied to the input*/
	public static void processAdd(FloatBuffer in, FloatBuffer out) {
		in.rewind();
		out.rewind();
		for (int z = 0; z < out.capacity(); z++)
			out.put(out.get(z) + in.get(z));
	}

	/** MIX in and out with gain applied to the input*/
	public static void processAdd(FloatBuffer in, float gain, FloatBuffer out) {
		if (1 == gain) {
			processAdd(in, out);
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

	public static void processMix(float[] in, FloatBuffer out) {
		out.get(workArea);
		out.rewind();
		for (z = 0; z < Constants.bufSize(); z++)
			out.put(workArea[z] + in[z]);
	}
	public static void processGain(FloatBuffer buffer, float gain) {
		buffer.rewind();
		for (z = 0; z < Constants.bufSize(); z++)
			buffer.put(buffer.get(z) * gain);

	}

}

