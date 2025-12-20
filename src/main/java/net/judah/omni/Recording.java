package net.judah.omni;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Vector;

import lombok.Getter;

/**	------------WavFile------------<br/>
 * Stereo Audio data of .wav File, a Loop or a (Drum)Sample, organized by Jack buffer. <br/>
 * Not lightweight, an RMS cache is also stored for add() and set() procedures.
 * <br/>
 * <br/><pre>
	Wav file IO class
 	A.Greensted
	http://www.labbookpages.co.uk/audio/javaWavFiles.html

	File format is based on the information from
	http://www.sonicspot.com/guide/wavefiles.html
	http://www.blitter.com/~russtopia/MIDI/~jglatt/tech/wave.htm </pre>*/
public class Recording extends Vector<float[][]> implements WavConstants {

	/* Unless specified otherwise, bring the incoming audio up to "line-level" */
	public static final float BOOST = 2.5f;

	@Getter private File file; 					// can be null (looper)
	private FileInputStream iStream;	// Input stream used for reading data
	private final byte[] buffer = new byte[DISK_BUFFER]; // local buffer for disk IO
	private int numFrames;				// Number of frames within the data section
	int numChannels = 2;				// 2 bytes unsigned, 0x0001 (1) to 0xFFFF (65,535)
	long blockAlign;					// 2 bytes unsigned, 0x0001 (1) to 0xFFFF (65,535)
	private int bufferPointer;			// Points to the current position in local buffer
	private int bytesRead;				// Bytes read after last read into local buffer
	private int frameCounter;			// Current number of frames read or written

	public Recording() { /* for looper */ }

	public Recording(Recording recording, int duplications) {
		int size = recording.size() * duplications;
		int x = 0;
		for (int j = 0; j < size; j++) {
			x++;
			if (x >= recording.size())
				x = 0;
			float[][] data = new float[2][JACK_BUFFER];
			AudioTools.copy(recording.get(x), data);
			add(data);
		}
	}

	public Recording (File file, float factor) throws IOException {
		load(file, factor);
	}

	public Recording (File file) throws IOException {
		load(file, BOOST);
	}

	public int load(File file) throws IOException {
		return load(file, 1f);
	}

	/**@return size of file in frames */
	public int load(File file, float factor) throws IOException {
		final int size = size();
		this.file = file;
		iStream = new FileInputStream(file);

		// Read the first 12 bytes of the file
		int bytesRead = iStream.read(buffer, 0, 12);
		if (bytesRead != 12) throw new IOException("Not enough wav file bytes for header");

		// Extract parts from the header
		long riffChunkID = getLE(buffer, 0, 4);
		int chunkSize = getLE(buffer, 4, 4);
		long riffTypeID = getLE(buffer, 8, 4);

		// Check the header bytes contains the correct signature
		if (riffChunkID != RIFF_CHUNK_ID) throw new IOException("Invalid Wav Header data, incorrect riff chunk ID");
		if (riffTypeID != RIFF_TYPE_ID) throw new IOException("Invalid Wav Header data, incorrect riff type ID");

// TODO	// Check that the file size matches the number of bytes listed in header
//		if (file.length() != chunkSize+8)
//			RTLogger.warn(this, "File size ( " + file.length()+ ") does not match Header chunk size " + (chunkSize + 8)
//					 + " diff: " + (file.length() - (chunkSize+8)));

		boolean foundFormat = false;
		boolean foundData = false;

		// Search for the Format and Data Chunks
		while (true) {
			// Read the first 8 bytes of the chunk (ID and chunk size)
			bytesRead = iStream.read(buffer, 0, 8);
			if (bytesRead == -1) throw new IOException("Reached end of file without finding format chunk");
			if (bytesRead != 8) throw new IOException("Could not read chunk header");

			// Extract the chunk ID and Size
			long chunkID = getLE(buffer, 0, 4);
			chunkSize = getLE(buffer, 4, 4);

			// Word align the chunk size
			// chunkSize specifies the number of bytes holding data. However,
			// the data should be word aligned (2 bytes) so we need to calculate
			// the actual number of bytes in the chunk
			long numChunkBytes = (chunkSize%2 == 1) ? chunkSize+1 : chunkSize;

			if (chunkID == FMT_CHUNK_ID) {
				// Flag that the format chunk has been found
				foundFormat = true;

				// Read in the header info
				bytesRead = readFormat();
				// Account for number of format bytes and skip over any extra format bytes
				numChunkBytes -= 16;
				if (numChunkBytes > 0) iStream.skip(numChunkBytes);
			}
			else if (chunkID == DATA_CHUNK_ID) {
				if (!foundFormat) throw new IOException("Data chunk found before Format chunk");

				// Check that the chunkSize (wav data length) is a multiple of the
				// block align (bytes per frame)
				if (chunkSize % blockAlign != 0) throw new IOException("Data Chunk size is not multiple of Block Align");

				// Calculate the number of frames
				numFrames = (int) (chunkSize / blockAlign);

				// Flag that we've found the wave data chunk
				foundData = true;

				break;
			}
			else {
				// If an unknown chunk ID is found, just skip over the chunk data
				iStream.skip(numChunkBytes);
			}
		}

		if (foundData == false) throw new IOException("Did not find a data chunk");
		bufferPointer = 0;
		frameCounter = 0;
		bytesRead = 0;
		int jackFrame = 0;
		// Create a buffer of jack frame size
		float[] buffer = new float[JACK_BUFFER * STEREO];
		int framesRead;
		do {
            // Read frames into buffer
            framesRead = readFrames(buffer, 0, JACK_BUFFER);

            float[][] frame = new float[2][JACK_BUFFER];
            if (numChannels == 2)
	            // cycle through frames and put them in loop Recording format
	            for (int i = 0 ; i < framesRead * STEREO; i += 2) {
	            	int jack = i / 2;
	            	frame[LEFT][jack] = factor * buffer[i];
	            	frame[RIGHT][jack] = factor * buffer[i + 1];
	            }
            else
	            for (int i = 0 ; i < framesRead; i++) {
	            	frame[LEFT][i] = factor * buffer[i];
	            	frame[RIGHT][i] = frame[LEFT][i];
	            }

            if (jackFrame < size)
            	set(jackFrame, frame);
            else add(frame);
            jackFrame++;
        } while (framesRead != 0);

		if (iStream != null) {
			iStream.close();
			iStream = null;
		}
		return jackFrame;
	}

	private int readFrames(float[] sampleBuffer, int offset, int numFramesToRead) throws IOException {
		for (int f = 0 ; f < numFramesToRead ; f++) {
			if (frameCounter == numFrames) return f;
			for (int c=0 ; c<numChannels ; c++) {
				sampleBuffer[offset] = readSample() / FLOAT_SCALE;
				offset ++;
			}
			frameCounter ++;
		}
		return numFramesToRead;
	}

	private long readSample() throws IOException {
		long val = 0;
		for (int b=0 ; b<SAMPLE_BYTES ; b++) {
			if (bufferPointer == bytesRead)  {
				int read = iStream.read(buffer, 0, DISK_BUFFER);
				if (read == -1) throw new IOException("Not enough data available");
				bytesRead = read;
				bufferPointer = 0;
			}
			int v = buffer[bufferPointer];
			if (b < SAMPLE_BYTES - 1 || SAMPLE_BYTES == 1) v &= 0xFF;
			val += v << (b * 8);
			bufferPointer ++;
		}
		return val;
	}

	private int readFormat() throws IOException {
		int result = iStream.read(buffer, 0, 16);

		// Check this is uncompressed data
		int compressionCode = getLE(buffer, 0, 2);
		if (compressionCode != 1) throw new IOException("Compression Code " + compressionCode + " not supported");

		// Extract the format information
		numChannels = getLE(buffer, 2, 2);
		long srate = getLE(buffer, 4, 4);
		if (srate != S_RATE)
			throw new IOException("Sample rate(" + S_RATE + ") vs: " + srate + " " + file.getAbsolutePath());
		blockAlign = getLE(buffer, 12, 2);

		int bits = getLE(buffer, 14, 2);
		if (bits != VALID_BITS)
			throw new IOException("Bit Depth: " + bits + " (expected: " + VALID_BITS + ") " + file.getAbsolutePath());
		if (numChannels == 0) throw new IOException("Number of channels specified in header is equal to zero");
		if (blockAlign == 0) throw new IOException("Block Align specified in header is equal to zero");
		if (SAMPLE_BYTES * numChannels != blockAlign)
			throw new IOException("Block Align does not agree with bytes required for VALID_BITS and number of channels");
		return result;
	}

	public static long sampleToMillis(long samplePosition) {
		return (long) ((samplePosition / (float)S_RATE) * 1000f);
	}

	/**@param supplied buffer
	 * @param pos
	 * @param numBytes
	 * @return little endiean data to long from pos in the supplied buffer */
	static int getLE(byte[] buffer, int pos, int numBytes) {
		numBytes --;
		pos += numBytes;
		int val = buffer[pos] & 0xFF;
		for (int b=0 ; b<numBytes ; b++)
			val = (val << 8) + (buffer[--pos] & 0xFF);
		return val;
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
