package net.judah.synth.taco;

import java.util.Random;

import lombok.Getter;

/** inspired by: https://github.com/johncch/MusicSynthesizer  WaveTable
 * Under the MIT license. Copyright (c) 2010, Chong Han Chua, Veronica Borges */
public enum Shape { // TODO Amp/Duty/PULSE_MOD

	SIN,
	SQR,
	TRI,
	SAW,
	RND;

	public static final float DUTY_FACTOR = 0.9f; // SQR and RND
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
		final static float[] RND = rnd(new float[LENGTH]);
		final static float[][] list = new float[][] {SIN, SQR, TRI, SAW, RND};

		public static float[] sin(float[] wave) {
			float dt = (float)(2f * Math.PI / LENGTH);
			for(int i = 0; i < LENGTH; i++)
				wave[i] = (float)Math.sin(i * dt);
			return wave;
		}

		public static float[] sqr(float[] wave) {
			for(int i = 0; i < LENGTH; i++)
				wave[i] = (i < LENGTH / 2) ? DUTY_FACTOR : -DUTY_FACTOR;
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

		public static float[] rnd(float[] wave) {
			Random random = new Random(System.currentTimeMillis());
			for (int i = 0; i < wave.length; i++)
				wave[i] = random.nextFloat() * 2 * DUTY_FACTOR - DUTY_FACTOR; // ??
			return wave;
		}
	}


}