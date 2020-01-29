package net.judah.looper;

import java.nio.FloatBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.judah.RTLogger;
import net.judah.jack.mixers.Merge;
import net.judah.jack.mixers.SimpleMixer;

@SuppressWarnings("serial") @RequiredArgsConstructor
public class ClipBoard extends LinkedList<Recording> {

	private final int channels;
	private final int bufSize;
	private final AtomicInteger counter = new AtomicInteger();
	private int loopCount;

	Merge mixer = new SimpleMixer();

	float[][] ary;
	Recording live = new Recording();
	Recording preload = new Recording();
	@Getter protected Integer loopLength;

	public void stopRecording(long jackTime) {

		push(live);
		if (loopLength == null)
			loopLength = get(0).size();

		live = preload;
		preload = new Recording();
		new Thread() {
			@Override public void run() {
				for (int i = 0; i < get(0).size(); i++) {
					live.add(new float[channels][bufSize]);
				}
			}
		}.start();

		counter.set(0);
		RTLogger.log(this, "First loop made. " + loopLength + " frames.");


	}
	public void startRecording(long jackTime) {
		RTLogger.log(this, "startRecording " + jackTime + " " + size());
	}


	public void record(float[][] in) {
		if (isEmpty()) {
			live.add(in);
			preload.add(new float[channels][bufSize]);
		}
		else {
			throw new RuntimeException("Recording already exists");
		}
	}

	private int x, y;
	private FloatBuffer buf;
	/**save the mixed sound */

	/**@param outputs the buffers to receive the recorded audio
	 * @return the recorded audio */
	public float[][] play(List<FloatBuffer> outputs, int nframes) {
		float[][] tape = next();
		for (x = 0; x < outputs.size(); x++) {
			buf = outputs.get(x);
			buf.rewind();
			for (y = 0; y < nframes; y++) {
				buf.put(tape[x][y] /** gain*/);
			}
		}
		return tape;
	}
//
//	////////////////////////////
//	// Collection interface
//	@Override
//	public int size() {
//		return live.size();
//	}
//
	private float[][] result;
	private int current;

	protected float[][] next() {

		result = get(0).get(counter.intValue());
		current = counter.incrementAndGet();
		if (current == get(0).size()) { // LOOP!
			counter.set(0);
			++loopCount;
			RTLogger.log(this, "looping, count = " + loopCount);
		}
		return result;
	}

//	private int previous() {
//		int result = counter.intValue() - 1;
//		if (result < 0) result = get(0).size();
//		return result;
//	}


}



//public void overdub(float[][] tape, float[][] mixer) {
//for (x = 0; x < tape.length; x++) {
//	for (y = 0; y < tape[x].length; y++) {
//
//	}
//}
//// mixer.merge(next(), in, out);
//}
//public static void overdub(float[][] tape, List<FloatBuffer> inputs, float[][] target) {
//for (int x = 0; x < tape.length; x++) {
//	buf = inputs.get(x);
//	for (y = 0; y < tape[x].length; y++) {
//		target[x][y] = tape[x][y] + buf.get();
//	}
//}
//
//live.add(previous(), target);
//}
