package net.judah.synth;

import lombok.Getter;

/** inspired by: https://github.com/johncch/MusicSynthesizer  WaveTable 
 * Under the MIT license. Copyright (c) 2010, Chong Han Chua, Veronica Borges */
public enum Shape { // TODO Amp/Duty, PULSE_MOD, NOISE

	SIN, 
	SQR, 
	TRI, 
	SAW;
	
	public final static int BITS = 16;
	public final static int LENGTH = 1 << (BITS - 1);
	public final static int MASK = LENGTH - 1;

	@Getter private final float[] wave;

	Shape() {
		wave = Waves.list[ordinal()];
	}
	
	public static class Waves {
		
		final static float[] SIN = sin(new float[LENGTH]);
		final static float[] SQR = sqr(new float[LENGTH]);
		final static float[] TRI = tri(new float[LENGTH]);
		final static float[] SAW = saw(new float[LENGTH]);
		final static float[][] list = new float[][] {SIN, SQR, TRI, SAW};
		
		public static float[] sin(float[] wave) {
			float dt = (float)(2f * Math.PI / LENGTH);
			for(int i = 0; i < LENGTH; i++) 
				wave[i] = (float)Math.sin(i * dt);
			return wave;
		}
	
		public static float[] sqr(float[] wave) {
			for(int i = 0; i < LENGTH; i++) 
				wave[i] = (i < LENGTH / 2) ? 0.9f : -0.9f;
			return wave;
		}
	
		public static float[] tri(float[] wave) {
			float dt = 2f / LENGTH;
			for(int i = 0; i < LENGTH; i++) 
				wave[i] = 1.0f - i*dt;
			return wave;
		}

		public static float[] saw(float[] wave) {
			float dt = 4.0f / LENGTH;
			for(int i = 0; i < LENGTH; i++) {
				if(i < LENGTH / 2) {
					wave[i] = 1f - i * dt;
				} else {
					wave[i] = -3f + i*dt;
				}
			}
			return wave;
		}	
	}

	
}