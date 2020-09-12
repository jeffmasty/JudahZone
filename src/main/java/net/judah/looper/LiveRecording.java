package net.judah.looper;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import net.judah.jack.AudioTools;

/** threaded to accept live stream */
public class LiveRecording extends Recording {

	transient private final BlockingQueue<float[][]> newQueue = new LinkedBlockingQueue<>();
	transient private final BlockingQueue<float[][]> oldQueue = new LinkedBlockingQueue<>();
	transient private final BlockingQueue<Integer> locationQueue = new LinkedBlockingQueue<>(); 

	public LiveRecording() {
		new Runner().start();
	}
	
	class Runner extends Thread {
		@Override public void run() {
			try {
				while (true) {
					// MIX 
					float[][] in = newQueue.take();
					float[][] old = oldQueue.take();
					set(locationQueue.take(), AudioTools.overdub(in, old));
				}
			} catch (InterruptedException e) {  }
		}
	}

	/** get off the process thread */
	public void dub(float[][] newBuffer, float[][] oldBuffer, int location) {
		newQueue.add(newBuffer);
		oldQueue.add(oldBuffer);
		locationQueue.add(location);
	}
}
