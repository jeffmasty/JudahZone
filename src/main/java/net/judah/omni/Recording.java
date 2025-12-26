package net.judah.omni;

import java.io.File;
import java.io.IOException;
import java.util.Vector;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import net.judah.looper.FromDisk;

/**	------------WavFile------------<br/>
 * Stereo Audio data of .wav File, a Loop or a (Drum)Sample, organized by Jack buffer. <br/>
 * <br/>
 * <br/><pre>
	Wav file IO class
 	A.Greensted
	http://www.labbookpages.co.uk/audio/javaWavFiles.html

	File format is based on the information from
	http://www.sonicspot.com/guide/wavefiles.html
	http://www.blitter.com/~russtopia/MIDI/~jglatt/tech/wave.htm </pre>*/
public class Recording extends Vector<float[][]> implements WavConstants {

	public Recording() { /* for looper */ }

	/** Convenience: use default BOOST mastering when not specified. */
	public Recording(File f) {
		this(f, 1);
	}

	/** Read file and chunkify while the stream is being decoded.
	 *  This constructor will block until the file has been fully read.
	 *
	 *  @param f input audio file
	 *  @param mastering positive multiplier applied to samples (clamped to [-1,1])
	 */
	public Recording(File f, float mastering) {
		if (f == null) return;

		try {
			AudioDispatcher dispatcher = AudioDispatcherFactory.fromPipe(
					f.getAbsolutePath(), S_RATE, JACK_BUFFER, 0);

			final int channels = dispatcher.getFormat().getChannels();
			// current assembling block
			float[] leftBlock = new float[JACK_BUFFER];
			float[] rightBlock = new float[JACK_BUFFER];

			dispatcher.addAudioProcessor(new AudioProcessor() {
				@Override public boolean process(AudioEvent audioEvent) {
					float[] buf = audioEvent.getFloatBuffer();
					int posInBlock = 0;
					if (channels <= 1) {
						// mono -> duplicate
						for (float v : buf) {
							float s = clamp(v * mastering);
							leftBlock[posInBlock] = s;
							rightBlock[posInBlock] = s;
							posInBlock++;
							if (posInBlock >= JACK_BUFFER) {
								float[][] block = new float[2][JACK_BUFFER];
								System.arraycopy(leftBlock, 0, block[0], 0, JACK_BUFFER);
								System.arraycopy(rightBlock, 0, block[1], 0, JACK_BUFFER);
								Recording.this.add(block);

								posInBlock = 0;
							}
						}
						return true;
					}

					// Heuristic: Tarsos sometimes supplies non-interleaved buffers
					if (buf.length % 2 == 0) {
						int half = buf.length / 2;
						// treat as non-interleaved halves if plausible
						if (half <= JACK_BUFFER * 8) { // cheap heuristic to avoid false positives
							for (int i = 0; i < half; i++) {
								float l = clamp(buf[i] * mastering);
								float r = clamp(buf[half + i] * mastering);
								leftBlock[posInBlock] = l;
								rightBlock[posInBlock] = r;
								posInBlock++;
								if (posInBlock >= JACK_BUFFER) {
									float[][] block = new float[2][JACK_BUFFER];
									System.arraycopy(leftBlock, 0, block[0], 0, JACK_BUFFER);
									System.arraycopy(rightBlock, 0, block[1], 0, JACK_BUFFER);
									Recording.this.add(block);
									posInBlock = 0;
								}
							}
							return true;
						}
					}

					// fallback: interleaved LRLR...
					for (int i = 0; i + 1 < buf.length; i += 2) {
						float l = clamp(buf[i] * mastering);
						float r = clamp(buf[i + 1] * mastering);
						leftBlock[posInBlock] = l;
						rightBlock[posInBlock] = r;
						posInBlock++;
						if (posInBlock >= JACK_BUFFER) {
							float[][] block = new float[2][JACK_BUFFER];
							System.arraycopy(leftBlock, 0, block[0], 0, JACK_BUFFER);
							System.arraycopy(rightBlock, 0, block[1], 0, JACK_BUFFER);
							Recording.this.add(block);
							posInBlock = 0;
						}
					}
					return true;
				}

				@Override
				public void processingFinished() { // TODO streaming
				}
			});

			// run decoding (blocks until finished)
			dispatcher.run();

		} catch (Exception e) {
			// On error: leave Recording empty rather than throwing from constructor.
			e.printStackTrace();
		}
	}


	public static Recording loadInternal(File f) throws IOException {
		return loadInternal(f, 1f);
	}


	public static Recording loadInternal(File f, float mastering) throws IOException {
		Recording result = new Recording();
		new FromDisk().load(f, mastering, result);
		return result;
	}

	private static float clamp(float s) {
	    return Math.max(-1f, Math.min(1f, s));
	}

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

	/**Create a new Recording containing at most {@code maxFrames} blocks (jack buffers).  By Reference, not copy.
     * @param maxFrames maximum number of jack-buffer blocks to include in the returned Recording
     * @return a new Recording truncated/padded to exactly up to maxFrames blocks */
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

	/** copy from a channel into destination from startSample
	 * @param destination array of desired size to copy
	 * @param startSample index of the initial sample to copy, ignoring buffer size.
	 * @throws ArrayIndexOutOfBoundsException */
	public void getSamples(long startSample, float[] destination, int ch) {
	    int startBuf = (int) (startSample / JACK_BUFFER);
	    int offset = (int) (startSample % JACK_BUFFER);
	    int total = destination.length;
	    int destIndex = 0;

	    while (destIndex < total) {
	        // Get the current buffer from the source
	        float[] buffer = get(startBuf)[ch];
	        // Calculate the number of samples to copy from the current buffer
	        int samplesToCopy = Math.min(JACK_BUFFER - offset, total - destIndex);

	        System.arraycopy(buffer, offset, destination, destIndex, samplesToCopy);
	        // Update the destination index and offset for the next iteration
	        destIndex += samplesToCopy;
	        offset = 0; // Reset offset for the next buffer
	        startBuf++; // Move to the next buffer in the source
	    }
	}

	public float[][] getSamples(int idx, int length) {
		float[] l = new float[length];
		getSamples(idx, l, LEFT);
		float[] r = new float[length];
		getSamples(idx, r, RIGHT);
		return new float[][] {l, r};
	}


	/** copy from left channel into destination from startSample
	 * @param destination array of desired size to copy
	 * @throws ArrayIndexOutOfBoundsException */
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



///** Frame chop complete streams */
//public Recording(float[] left, float[] right) {
//    // Defensive lengths
//    final int lenL = left != null ? left.length : 0;
//    final int lenR = right != null ? right.length : 0;
//    final int maxLen = Math.max(lenL, lenR);
//
//    if (maxLen == 0) {
//        // nothing to add
//        return;
//    }
//
//    final int blocks = (maxLen + JACK_BUFFER - 1) / JACK_BUFFER;
//    this.ensureCapacity(blocks);
//
//    for (int b = 0; b < blocks; b++) {
//        int start = b * JACK_BUFFER;
//        float[][] block = new float[2][JACK_BUFFER];
//
//        for (int i = 0; i < JACK_BUFFER; i++) {
//            int idx = start + i;
//
//            float l = 0f;
//            float r = 0f;
//
//            if (idx < lenL) l = left[idx];
//            if (idx < lenR) r = right[idx];
//
//            if (lenL == 0 && lenR > 0)
//                l = r; // left missing -> use right
//            else if (lenR == 0 && lenL > 0)
//                r = l; // right missing -> use left
//
//            block[0][i] = l;
//            block[1][i] = r;
//        }
//
//        this.add(block);
//    }
//}

