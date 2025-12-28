package net.judah.gui.scope;

import static net.judah.util.Constants.LEFT;
import static net.judah.util.Constants.RIGHT;

import net.judah.util.AudioTools;

public record RMS(float rms, float peak, float amplitude) {

	public static RMS analyze(float[] channel) {
	    float sumPositive = 0;
	    float sumNegative = 0;
	    int countPositive = 0;
	    int countNegative = 0;
	    float min = Float.MAX_VALUE;
	    float max = Float.MIN_VALUE;

	    for (float val : channel) {
	        if (val > 0) {
	            sumPositive += val;
	            countPositive++;
	        } else if (val < 0) {
	            sumNegative += val;
	            countNegative++;
	        }
	        if (val < min)
	            min = val;
	        if (val > max)
	            max = val;
	    }

	    float avgPositive = countPositive > 0 ? sumPositive / countPositive : 0;
	    float avgNegative = countNegative > 0 ? sumNegative / countNegative : 0;
	    float rms = AudioTools.rms(channel);
	    float peak = hiLo(max, min);
	    float amp = hiLo(avgPositive, avgNegative);
	    return new RMS(rms, peak, amp);
	}

	public static RMS analyze(float[][] in) {
		RMS left = analyze(in[LEFT]);
		RMS right = analyze(in[RIGHT]);
		return left.rms > right.rms ? left : right;
	}

	private static float hiLo(float pos, float neg) {
		if (pos > Math.abs(neg))
			return pos;
		return Math.abs(neg);
	}

}
