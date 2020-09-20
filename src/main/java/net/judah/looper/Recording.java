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
import net.judah.jack.AudioTools;

public class Recording extends Vector<float[][]> {
	
	public Recording() {
		new Runner().start();
	}

	public Recording(boolean startListeners) {
		if (startListeners)
			new Runner().start();
	}
	
	@Getter @Setter private String notes;

	transient private final BlockingQueue<float[][]> newQueue = new LinkedBlockingQueue<>();
	transient private final BlockingQueue<float[][]> oldQueue = new LinkedBlockingQueue<>();
	transient private final BlockingQueue<Integer> locationQueue = new LinkedBlockingQueue<>(); 
	
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
		notes = toCopy.notes;
	}
	
	public static Recording readAudio(String filename) throws IOException, ClassNotFoundException {
		ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filename));
		Recording result = (Recording)ois.readObject(); 
		ois.close();
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
