package net.judah.gui.waves;

import static net.judah.omni.AudioTools.rms;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import lombok.NoArgsConstructor;
import net.judah.omni.Recording;
import net.judah.omni.Threads;

/** shadows a Recording with cached RMS values */
@NoArgsConstructor
public class RMSRecording extends Recording {
	public static record RMS(float left, float right) {}

	protected final ArrayList<RMS> rms = new ArrayList<RMS>();

    public RMSRecording(File dotWav) throws IOException {
    	super();
    	load(dotWav, 1f);
    }

	/** @return pre-calculated RMS value for the buffer index */
	public RMS getRms(int frame) {
		return rms.get(frame);
	}

	@Override
	public synchronized float[][] set(int index, float[][] buffer) {
		float[][] old = super.set(index, buffer);
		rms.set(index, new RMS(rms(buffer[LEFT]), rms(buffer[RIGHT])));
		return old;
	}

	@Override
	public void duplicate(int frames) {
		super.duplicate(frames);
		for (int i = 0; i < frames; i++)
			add(get(i));
	}

	@Override
	public synchronized float[][] removeFirst() {
		float[][] result = super.removeFirst();
		rms.removeFirst();
		return result;
	}

	@Override
	public synchronized boolean add(float[][] buffer) {
		super.add(buffer);
		rms.add(new RMS(rms(buffer[LEFT]), rms(buffer[RIGHT])));
		return true;
	}

//	@Override
	public void catchUp(int newSize) {
    	Threads.execute(() -> {
    		for (int i = size(); i < newSize; i++) {
    			add(new float[STEREO][JACK_BUFFER]);
    			rms.add(new RMS(0, 0));
    		}});
	}

}
