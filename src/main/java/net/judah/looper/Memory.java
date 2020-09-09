package net.judah.looper;

import java.util.concurrent.LinkedBlockingQueue;

import net.judah.util.RTLogger;

/** supplies objects created in a side thread */
public class Memory {

	LinkedBlockingQueue<float[]> memory = new LinkedBlockingQueue<>();
	
	static final int PRELOAD = 2000;
	static final int THRESHOLD = (int)(PRELOAD * 0.9f);
	static final int RELOAD = PRELOAD - THRESHOLD;
	private final int channelCount;
	private final int bufferSize;
	int idx;
	
	public Memory(int numChannels, int bufferSize) {
		this.channelCount = numChannels;
		this.bufferSize = bufferSize;
		preload(PRELOAD);
	}

	public float[][] getArray(int portCount) {
		if (portCount == 0) return null;
		if (memory.size() < THRESHOLD)
			preload(RELOAD);
		try {
			float[][] result = new float[portCount][];
			for (idx = 0; idx < portCount; idx++) {
				result[idx] = memory.take();
			}
			return result;
			
		} catch (InterruptedException e) {
			RTLogger.warn(this, "Memory depleted");
			assert false;
			return new float[channelCount][bufferSize];
		}
	}
	
	public float[] get() {
		if (memory.size() < THRESHOLD) 
			preload(RELOAD);
		try {
			return memory.take();
		} catch (InterruptedException e) {
			RTLogger.warn(this, "Memory depleted");
			assert false;
			return new float[bufferSize];
		}

	}
	
	public float[][] getArray() {
		if (memory.size() < THRESHOLD)
			preload(RELOAD);
		try {
			float[][] result = new float[channelCount][];
			for (idx = 0; idx < channelCount; idx++)
				result[idx] = memory.take();
			return result;
		} catch (InterruptedException e) {
			RTLogger.warn(this, "Memory depleted");
			assert false;
			return new float[channelCount][bufferSize];
		}
	}

	private void preload(final int amount) {
		new Thread() {
			@Override
			public void run() {
				for (int i = 0; i < amount; i++) {
					memory.add(new float[bufferSize]);
				}
			}
		}.start();
	}

	/** current number of preloaded instances */
	public int elements() {
		return memory.size();
	}

}
