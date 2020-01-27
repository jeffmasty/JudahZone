package net.judah.jack.mixers;

import java.nio.FloatBuffer;
import java.util.List;


/*TODO
from: https://github.com/xthexder/go-jack/blob/master/README.md
func process(nframes uint32) int {
	samples := Port.GetBuffer(nframes)
	nsamples := float64(len(samples))
	for i := range samples {
		samples[i] = jack.AudioSample(math.Sin(float64(i)*math.Pi*20/nsamples) / 2)
	}
	return 0
}
 */
public class SineWaveMixer implements Merge {



	public SineWaveMixer() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void merge(List<FloatBuffer> in1, float gain1, float[][] in2, float gain2, List<FloatBuffer> out) {
		// TODO Auto-generated method stub

	}

	@Override
	public void merge(List<FloatBuffer> input1, float[][] input2, List<FloatBuffer> output) {
		// TODO Auto-generated method stub

	}

	@Override
	public void merge(List<FloatBuffer> inputs, float[][] tape, float[][] recording) {
		// TODO Auto-generated method stub

	}

	@Override
	public void merge(float[][] a, float[][] b, float[][] out) {
		// TODO Auto-generated method stub

	}

}
