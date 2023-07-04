package net.judah.looper;

import java.util.concurrent.LinkedBlockingQueue;

import net.judah.util.Constants;
import net.judah.util.RTLogger;

/** Creates a steady supply of float arrays in a side thread to be used for recording by realtime thread */
public class Memory {

	static final int PRELOAD = 4000;
	static final int THRESHOLD = (int)(PRELOAD * 0.9f);
	static final int RELOAD = (int)(PRELOAD * 0.5f);

	private final LinkedBlockingQueue<float[]> memory = new LinkedBlockingQueue<>();
	private final int channelCount;
	private final int bufSize;
	
	public Memory(int numChannels, int bufferSize) {
		this.channelCount = numChannels;
		this.bufSize = bufferSize;
		preload(PRELOAD);
	}

	public float[][] getArray() {
		if (memory.size() < THRESHOLD)
			preload(RELOAD);
		try {
			float[][] result = new float[channelCount][];
			for (int idx = 0; idx < channelCount; idx++)
				result[idx] = memory.take();
			return result;
		} catch (InterruptedException e) {
			RTLogger.warn(this, "Memory depleted");
			return new float[channelCount][bufSize];
		}
	}

	private void preload(final int amount) {
		Constants.execute(()->{				
			for (int i = 0; i < amount; i++) 
				memory.add(new float[bufSize]);
		});
	}

	/** current number of preloaded instances */
	public int elements() {
		return memory.size();
	}

}
