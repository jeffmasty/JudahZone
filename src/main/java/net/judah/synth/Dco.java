package net.judah.synth;

import java.nio.FloatBuffer;

import lombok.Getter;
import net.judah.util.Constants;
/** inspired by: https://github.com/johncch/MusicSynthesizer/blob/master/src/com/fifthrevision/sound/Osc.java 
 *  Under the MIT license. Copyright (c) 2010, Chong Han Chua, Veronica Borges */
public class Dco {
	private final int SAMPLE_RATE = Constants.sampleRate();
	private final int BUF_SIZE = Constants.bufSize();
	
	@Getter private float freq;

	private float phase;
	private float cyclesPerSample;
	
	public Dco setHz(float freq) {
		this.freq = freq;
		if (freq > 0) {
			cyclesPerSample = freq/SAMPLE_RATE;
		}
		return this;
	}
	
	/* See https://github.com/johncch/MusicSynthesizer/blob/master/src/com/fifthrevision/sound/Osc.java  #render() */
	public void process(float amp, float[] wave, FloatBuffer output) {
		float scaled, fraction;
		int index;
		float[] mono = output.array();
		for(int i = 0; i < BUF_SIZE; i++) {
			scaled = phase * Shape.LENGTH;
			fraction = scaled-(int)scaled;
			index = (int)scaled;
			mono[i] += amp * (1.0f - fraction) * wave[index & Shape.MASK] + 
					amp * + fraction * wave[(index+1) & Shape.MASK];
			phase = phase + cyclesPerSample - (int)phase;
		}
	}

	
}
