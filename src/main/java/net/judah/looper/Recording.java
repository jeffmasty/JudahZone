package net.judah.looper;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Vector;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import lombok.Getter;
import lombok.Setter;
import net.judah.midi.JudahMidi;
import net.judah.util.AudioTools;

public class Recording extends Vector<float[][]> {

	@Getter @Setter private String notes;

	@Getter private long creationTime = System.currentTimeMillis();
	private BlockingQueue<float[][]> newQueue;
	private BlockingQueue<float[][]> oldQueue;
	private BlockingQueue<Integer> locationQueue;
	private Runner runner;

	public boolean isListening() {
		return newQueue != null;
	}

	public void startListeners() {
		newQueue = new LinkedBlockingQueue<>();
		oldQueue = new LinkedBlockingQueue<>();
		locationQueue = new LinkedBlockingQueue<>();
		runner = new Runner();
		runner.start();
	}

	public Recording(boolean startListeners) {
		if (startListeners) {
			startListeners();
		}
	}

	/** deep copy the 2D float array */
	Recording(Recording toCopy, boolean startListeners) {
		this(startListeners);
		float[][] copy;
		for (float[][] buffer : toCopy) {
		    copy = new float[buffer.length][];
		    for (int i = 0; i < buffer.length; i++) {
		        copy[i] = buffer[i].clone();
		    }
		    add(copy);
		}
		notes = toCopy.notes;
	}

	/** create empty recording of size */
	public Recording(int size, boolean startListeners) {
		this(startListeners);
		int bufferSize = JudahMidi.getInstance().getBufferSize();
		for (int j = 0; j < size; j++) {
			float[][] data = new float[2][bufferSize];
			AudioTools.processSilence(data);
			add(data);
		}
	}

	public static Recording readAudio(String filename) throws IOException, ClassNotFoundException {
		ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filename));
		Recording result = (Recording)ois.readObject();
		ois.close();
		result.startListeners();
		return result;
	}

	public void saveAudio(String filename) throws IOException {
		ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filename));
		oos.writeObject(this);
		oos.close();
	}

	class Runner extends Thread {
		@Override public void run() {
			try {
			while (true) {
				// MIX
				set(locationQueue.take(), AudioTools.overdub(newQueue.take(), oldQueue.take()));
			}} catch (InterruptedException e) {  }
		}
	}

	/** get off the process thread */
	public void dub(float[][] newBuffer, float[][] oldBuffer, int location) {
		assert newBuffer != null;
		assert oldBuffer != null;
		assert runner != null;
		assert newQueue != null;

		newQueue.add(newBuffer);
		oldQueue.add(oldBuffer);
		locationQueue.add(location);
	}

	public void close() {
		if (runner != null) {
			runner.interrupt();
			runner = null;
		}
	}

}
