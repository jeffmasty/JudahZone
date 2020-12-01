package net.judah.jack.mixers;

import java.nio.FloatBuffer;
import java.util.List;

public class SimpleMixer implements Merge {

	@Override
	public void merge(List<FloatBuffer> input1, float gain1, float[][] input2, float gain2, List<FloatBuffer> output) {
		if (gain1 > 0.99 && gain1 < 1.01 && gain2 > 0.99 && gain2 < 1.01) {
			merge(input1, input2, output);
			return;
		}
		FloatBuffer in, out;
		for (int i = 0; i < output.size(); i++) {
			in = input1.get(i);
			out = output.get(i);
			for (int j = 0; j < out.capacity(); j++)
				out.put( in.get() * gain1 + input2[i][j] * gain2);
		}
	}

	@Override
	public void merge(List<FloatBuffer> inputs, float[][] tape, List<FloatBuffer> output) {
		FloatBuffer in, out;
		for (int i = 0; i < output.size(); i++) {
			in = inputs.get(i);
			out = output.get(i);
			for (int j = 0; j < out.capacity(); j++)
				out.put( tape[i][j] + in.get() );
		}
	}

	@Override
	public void merge(List<FloatBuffer> inputs, float[][] tape, float[][] recording) {
		FloatBuffer in;
		for (int i = 0; i < recording.length; i++) {
			in = inputs.get(i);
			for (int j = 0; j < in.capacity(); j++)
				recording[i][j] = tape[i][j] + in.get() ;
		}


	}

	@Override
	public void merge(float[][] a, float[][] b, float[][] out) {
//		assert a.length > 0;
//		assert a[0].length > 0;
//		assert b.length > 0;
//		assert b[0].length > 0;
//		assert out.length > 0;
//		assert out[0].length > 0;
		for (int i = 0; i < out.length; i++) {
			for (int j = 0; j < out[i].length; j++) {
				out[i][j] = a[i][j] + b[i][j];
			}
		}

	}

}
