package net.judah.looper;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Vector;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
public class Recording extends Vector<float[][]> {
	
	@Getter @Setter private String notes;
	
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

	
}
