package net.judah.api;

import java.io.File;
import java.io.IOException;
import java.util.Vector;

import net.judah.util.AudioTools;
import net.judah.util.FromDisk;
import net.judah.util.WavConstants;

/**Uncompressed Stereo Audio (.wav File, Loop, Sample) organized by Jack buffer.*/
public class Recording extends Vector<float[][]> implements WavConstants {

	public Recording() { /* container only */ }

	/** Convenience: use default mastering when not specified. */
	public Recording(Recording recording, int duplications) {
		int size = recording.size() * duplications;
		int x = 0;
		for (int j = 0; j < size; j++) {
			x++;
			if (x >= recording.size())
				x = 0;
			add(AudioTools.clone(recording.get(x)));
		}
	}

	/** Load an uncompressed WAV into memory with default mastering (1.0). */
	public static Recording load(File f) throws IOException {
		return load(f, 1f);
	}

	/** Load an uncompressed WAV into memory with specified mastering. */
	public static Recording load(File f, float mastering) throws IOException {
		Recording result = new Recording();
		if (f == null) return result;

		// Caller is responsible for calling FromDisk.canLoadSafely(f) if desired.
		new FromDisk().load(f, mastering, result);
		return result;
	}

	/** Internal helper kept for legacy callers; no safety checks. */
	public static Recording loadInternal(File f) throws IOException {
		return loadInternal(f, 1f);
	}

	/** Internal helper kept for legacy callers; no safety checks. */
	public static Recording loadInternal(File f, float mastering) throws IOException {
		return load(f, mastering);
	}

	/**Create a new Recording containing at most {@code maxFrames} blocks (jack buffers).  By Reference, not copy. */
	public Recording truncate(int maxFrames) {
		Recording out = new Recording();
		if (maxFrames <= 0) return out;

		int toCopy = Math.min(this.size(), maxFrames);
		// Reuse existing blocks by reference for efficiency
		for (int i = 0; i < toCopy; i++) {
			out.add(this.get(i));
		}

		// If we need to pad, determine a block length to use for zero blocks.
		if (out.size() < maxFrames) {
			int padBlockLen = 512; // default fallback
			if (!this.isEmpty()) {
				float[][] first = this.get(0);
				if (first != null && first.length >= 2) {
					float[] left = first[0];
					if (left != null) padBlockLen = left.length;
					else {
						float[] right = first[1];
						if (right != null) padBlockLen = right.length;
					}
				}
			}
			// Append zero-filled blocks until we reach maxFrames
			for (int i = out.size(); i < maxFrames; i++) {
				out.add(new float[][] { new float[padBlockLen], new float[padBlockLen] });
			}
		}
		return out;
	}

	public float seconds() {
		return size() / FPS;
	}

	/** copy from a channel into destination from startSample */
	public void getSamples(long startSample, float[] destination, int ch) {
	    int startBuf = (int) (startSample / JACK_BUFFER);
	    int offset = (int) (startSample % JACK_BUFFER);
	    int total = destination.length;
	    int destIndex = 0;

	    while (destIndex < total) {
	        float[] buffer = get(startBuf)[ch];
	        int samplesToCopy = Math.min(JACK_BUFFER - offset, total - destIndex);

	        System.arraycopy(buffer, offset, destination, destIndex, samplesToCopy);
	        destIndex += samplesToCopy;
	        offset = 0;
	        startBuf++;
	    }
	}

	public float[][] getSamples(int idx, int length) {
		float[] l = new float[length];
		getSamples(idx, l, LEFT);
		float[] r = new float[length];
		getSamples(idx, r, RIGHT);
		return new float[][] {l, r};
	}

	/** copy from left channel into destination from startSample */
	public void getSamples(int startSample, float[] destination) {
		getSamples(startSample, destination, LEFT);
	}

	/** copy an entire channel into a new 1-D array, erasing buffer boundaries */
	public float[] getLeft() {
		return getChannel(LEFT);
	}

	/** copy an entire channel into a new 1-D array, erasing buffer boundaries */
	public float[] getChannel(int ch) {
		int buffer = JACK_BUFFER;
		float[] result = new float[size() * buffer];
		for (int i = 0; i < size(); i++) {
			System.arraycopy(get(i)[ch], 0, result, i * buffer, buffer);
		}
		return result;
	}

	public void silence(int end) {
		if (end == 0)
			return;
		if (end > size())
			end = size();
		for (int frame = 0; frame < end; frame++)
			for (int ch = LEFT; ch < get(frame).length; ch++)
				zero(get(frame)[ch]);
	}

	private void zero(float[] channel) {
		for (int i = 0; i < channel.length; i++)
			channel[i] = 0f;
	}

	/** @return total bytes of one channel */
	public int clone(Recording rec) {
		clear();
		int x = 0;
		for (int j = 0; j < rec.size(); j++) {
			x++;
			if (x >= rec.size())
				x = 0;
			float[][] data = new float[STEREO][JACK_BUFFER];
			AudioTools.copy(rec.get(x), data);
			add(data);
		}
		return size() * WavConstants.JACK_BUFFER;
	}

	// copy from start of size frames
	public void duplicate(int frames) {
		for (int deficit = (2 * frames) - size(); deficit > 0; deficit--)
			add(new float[2][JACK_BUFFER]); // usually padded with Memory.java

		for (int i = 0; i < frames; i++) {
			AudioTools.copy(get(i), get(i + frames));
		}
	}
}