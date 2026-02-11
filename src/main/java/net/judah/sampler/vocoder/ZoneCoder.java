package net.judah.sampler.vocoder;

import judahzone.util.AudioTools;
import judahzone.util.Constants;
import net.judah.JudahZone;
import net.judah.channel.LineIn;

public abstract class ZoneCoder { // Wellen, RRack, Stenzel

	public static enum Coders { Wellen, Rakar, Stenzel }
	public static enum Settings { WET, PREAMP, GAIN, DECAY }

	protected static final int SR = Constants.sampleRate();
	protected static final float ISR = 1f / SR;
	protected static final int BUF_SIZE = Constants.bufSize();
	protected static final float TWO_PI = (float)(2f * Math.PI);


	private final float[] normalized = new float[BUF_SIZE];
	private final float[] output = new float[BUF_SIZE];

	/** Common parameters */
	protected float dryWet = 0.5f;      // 0.0 .. 1.0
	protected float inputGain = 1f;     // linear multiplier
	protected float gain = 1f;          // linear multiplier
	protected float decay = 0.99f;      // autocorrelation decay
	protected float preamp = 1f;

	public int getParamCount() {
		return Settings.values().length;
	}

	/** Set parameter by ordinal index (0-100 value).
	    Mappings:
	    - WET      : 0..100 -> 0.0..1.0 (linear)
	    - IN_GAIN  : 0..100 -> logarithmic, 50=unity
	    - OUT_GAIN : 0..100 -> logarithmic, 50=unity
	    - MUFFLE   : 0..100 -> decay 0.90..0.999 (linear) */
	public void set(int idx, int value) {
		int v = Math.max(0, Math.min(100, value));
		Settings s = Settings.values()[idx];
		switch (s) {
			case WET:
				dryWet = v / 100.0f;
				break;
			case PREAMP:
				inputGain = Constants.logarithmic(v, 0.1f, 10.0f);
				break;
			case GAIN:
				gain = Constants.logarithmic(v, 0.1f, 10.0f);
				break;
			case DECAY:
				decay = Constants.interpolate(v, 0, 100, 0.90f, 0.999f);
				break;
		}
	}

	/** Get parameter value as 0..100 integer using inverse mappings. */
	public int get(int idx) {
		Settings s = Settings.values()[idx];
		switch (s) {
			case WET:
				return Math.round(dryWet * 100.0f);
			case PREAMP:
				return Constants.reverseLog(inputGain, 0.1f, 10.0f);
			case GAIN:
				return Constants.reverseLog(gain, 0.1f, 10.0f);
			case DECAY:
				return Math.round(Constants.interpolate(decay, 0.90f, 0.999f, 0, 100));
			default:
				return 0;
		}
	}

	/** Mono in, mono out. */
	protected abstract void processImpl(float[] input, float[] output);

	/** Pull mono input, apply preamp + inputGain, process, mix wet/dry. */
	public void process(float[] outLeft, float[] outRight) {

		LineIn mic = JudahZone.getInstance().getChannels().getMic();
		if (mic == null)
			return;
		float[] dry = mic.getLeft();
		for (int i = 0; i < BUF_SIZE; i++)
			normalized[i] = dry[i] * preamp * inputGain;

		processImpl(normalized, output);

		// wet/dry mix with output gain
		float inverse = 1.0f - dryWet;
		for (int i = 0; i < BUF_SIZE; i++)
			output[i] = gain * (output[i] * dryWet + dry[i] * inverse);

		AudioTools.mix(output, outLeft);
		AudioTools.mix(output, outRight);
	}
}
