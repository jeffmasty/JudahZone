package net.judah.looper;

import java.util.Vector;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import net.judah.jack.AudioTools;


@SuppressWarnings("serial")
public class Recording extends Vector<float[][]>{

	private final BlockingQueue<float[][]> newQueue = new LinkedBlockingQueue<>();
	private final BlockingQueue<float[][]> oldQueue = new LinkedBlockingQueue<>();
	private final BlockingQueue<Integer> locationQueue = new LinkedBlockingQueue<>(); 
	
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
	
	public Recording() {
		new Runner().start();
	}

	/** deep copy the 2D float array */
	Recording(Recording toCopy) {
		float[][] copy;
		for (float[][] buffer : toCopy) {
		    copy = new float[buffer.length][];
		    for (int i = 0; i < buffer.length; i++) {
		        copy[i] = buffer[i].clone();
		    }
		    add(copy);
		}
	}
	
	
	/** get off the process thread */
	public void dub(float[][] newBuffer, float[][] oldBuffer, int location) {
		newQueue.add(newBuffer);
		oldQueue.add(oldBuffer);
		locationQueue.add(location);
	}

}
