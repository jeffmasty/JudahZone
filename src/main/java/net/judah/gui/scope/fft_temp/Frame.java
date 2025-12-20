package net.judah.gui.scope.fft_temp;

import java.util.Vector;

import lombok.RequiredArgsConstructor;
import net.judah.util.Constants;

/** A stereo buffer of float[]'s */
@RequiredArgsConstructor
public class Frame {
	public final float[] left;
	public final float[] right;

	/** blank stereo frame of length bufSize */
	public Frame() {
		left = new float[Constants.bufSize()];
		right = new float[Constants.bufSize()];
	}

	public boolean isMono() { return right == null; }

	public int getSize() { return left.length;	}

	public Vector<float[][]> oldSchool() {
		Vector<float[][]> result = new Vector<float[][]>();
		int size = Constants.bufSize();

		for (int i = 0; i < left.length / size; i++) {
			float[] l = new float[size];
			float[] r = new float[size];
			System.arraycopy(left, size * i, l, 0, size);
			System.arraycopy(right, size * i, r, 0, size);
			result.add(new float[][] {left, right});
		}
		return result;
	}

}
