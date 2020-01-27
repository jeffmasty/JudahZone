package net.judah.jack.mixers;

import java.nio.FloatBuffer;
import java.util.List;


public interface Merge {

	/* To mix sound waves digitally, you add each corresponding data point from the two files together.
	In other words, you take item 0 from byte array 1, and item 0 from byte array two, add them together,
	and put the resulting number in item 0 of your result array. Repeat for the remaining values.
	To prevent overload, you may need to divide each resulting value by two.
	The >> 1 is a bitwise right-shift; it effectively divides the resulting number by two.*/
	/* https://dsp.stackexchange.com/questions/3581/algorithms-to-mix-audio-signals-without-clipping
	 A + B + {
	    (|A| = A) = (|B| = B) = true: -AB;
	    (|A| = A) = (|B| = B) = false: AB;
	    else: 0
	}
	That is, if both A and B share a sign, apply a limiting offset. The magnitude of the offset is the
	product of A and B. The direction of the offset is opposite to that of A and B.
	If A and B do not share a sign, no limit is applied, as there is no way to overflow.*/
	/*Perhaps the wrong way to do it: http://www.vttoth.com/CMS/index.php/technical-notes/68
	 * http://atastypixel.com/blog/how-to-mix-audio-samples-properly-on-ios/comment-page-1/#comment-6310 */

//	/**
//	 *
//	 *
//	 *
//	 */

	/**merge, by a commodius vicus of recirculation, in1 and in2 into the out buffer
	* @param in1
	* @param gain1 between 0 and 2 (1 = no change)
	* @param in2
	* @param gain2 between 0 and 2 (1 = no change)
	* @param out*/
	void merge(List<FloatBuffer> in1, float gain1, float[][] in2, float gain2, List<FloatBuffer> out);

	/** merge without a volume/gain setting */
	void merge(List<FloatBuffer> input1, float[][] input2, List<FloatBuffer> output);

	void merge(List<FloatBuffer> inputs, float[][] tape, float[][] recording);


	void merge(float[][] a, float[][] b, float[][] out);


}



