package net.judah.util;

import java.util.List;

import lombok.Getter;
import net.judah.omni.WavConstants;

public class Constants {

	// TODO generalize
	public static int sampleRate() { return WavConstants.S_RATE; }
	public static int bufSize() { return WavConstants.JACK_BUFFER; }
	public static float fps() { return WavConstants.FPS; }
	/** Digital Interface name */
	@Getter static String di = "Komplete ";// "UMC1820 MIDI 1";
	public static final String LEFT_OUT = "system:playback_1";
	public static final String RIGHT_OUT = "system:playback_2";

	public static final String GUITAR_PORT = "system:capture_1";
	public static final String MIC_PORT = "system:capture_2";
	public static final String CRAVE_PORT = "system:capture_4";
	public static final String AUX_PORT = "system:capture_3";

	public static final String GUITAR = "Gtr";
	public static final String MIC = "Mic";
	public static final String BASS = "Bass";
	public static final String FLUID = "Fluid";
	public static final String MAIN = "Main";

    public static final int LEFT = 0;
	public static final int RIGHT = 1;
	public static final int STEREO = 2;
	public static final int MONO = 1;

	public static final String NL = System.lineSeparator();
	public static final String CUTE_NOTE = "♫ ";
	public static final String DOT_MIDI = ".mid";
	public static final int ASCII_ONE = 49;


	/** milliseconds between checking the update queue */
	public static final int GUI_REFRESH = 8;
	public static final float TO_100 = 0.7874f; // 127 <--> 100

	@Getter static float[] reverseLog = new float[100];
	static {
		for (int i = 0; i < reverseLog.length; i++)
			reverseLog[i] = logarithmic(i, 0, 1);
	}

    /**@param data2 0 to 127
     * @return data2 / 127 */
	public static float midiToFloat(int data2) {
		return data2 * 0.00787f;
	}

    public static float computeTempo(long millis, int beats) {
    	return bpmPerBeat(millis / (float)beats);
    }

    public static float bpmPerBeat(float msec) {
        return 60000 / msec;
    }

	public static long millisPerBeat(float beatsPerMinute) {
		return (long) (beatsPerMinute * 1.66666667e-5f); // 60000/BPM
	}

	public static float toBPM(long delta, int beats) {
		return 60000 / (delta / (float)beats);
	}

	public static Object ratio(int data2, List<?> input) {
        return input.get((int) ((data2 - 1) / (100 / (float)input.size())));
	}
	public static Object ratio(int data2, Object[] input) {
		return input[(int) ((data2 - 1) / (100 / (float)input.length))];
	}
	public static int ratio(long data2, long size) {
		return (int) (data2 / (100 / (float)size));
	}

	public static int rotary(int input, int size, boolean up) {
		 input += (up ? 1 : -1);
		 if (input >= size)
    		input = 0;
    	if (input < 0)
    		input = size - 1;
    	return input;
	}

    /** see https://stackoverflow.com/a/846249 */
	public static float logarithmic(int percent, float min, float max) {

		// percent will be between 0 and 100
		final int minp = 1;
		final int maxp = 100;
		assert percent <= max && percent >= min;

		// The result should be between min and max
		if (min <= 0) min = 0.0001f;
		double minv = Math.log(min);
		double maxv = Math.log(max);

		// calculate adjustment factor
		double scale = (maxv-minv) / (maxp-minp);
		return (float)Math.exp(minv + scale * (percent - minp));
	}

}
