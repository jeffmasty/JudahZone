package net.judah.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**	------------WavFile------------<br/>
 * load uncompressed Stereo Audio data of .wav File (Loop or a (Drum)Sample) organized by Jack buffer. <br/>
 * No memory or safety checks are performed here, unless using canLoadSafely().
 * <br/>
 * <br/><pre>
	Wav file IO class
 	A.Greensted
	http://www.labbookpages.co.uk/audio/javaWavFiles.html

	File format is based on the information from
	http://www.sonicspot.com/guide/wavefiles.html
	http://www.blitter.com/~russtopia/MIDI/~jglatt/tech/wave.htm </pre>*/
// Fast - no checks by default
public class FromDisk implements WavConstants {

	private FileInputStream iStream;	// Input stream used for reading data
	private final byte[] buffer = new byte[DISK_BUFFER]; // local buffer for disk IO
	private int numFrames;				// Number of frames within the data section
	int numChannels = 2;				// 2 bytes unsigned, 0x0001 (1) to 0xFFFF (65,535)
	long blockAlign;					// 2 bytes unsigned, 0x0001 (1) to 0xFFFF (65,535)
	private int bufferPointer;			// Points to the current position in local buffer
	private int bytesRead;				// Bytes read after last read into local buffer
	private int frameCounter;			// Current number of frames read or written

	/**@return size of file in frames */
	public int load(File file, float factor, Recording into) throws IOException {
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

		// Check that the file size matches the number of bytes listed in header
		if (file.length() != chunkSize + 8)
			RTLogger.debug(this, "File size ( " + file.length()+ ") does not match Header chunk size " + (chunkSize + 8)
					+ " diff: " + (file.length() - (chunkSize+8)));

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
			long numChunkBytes = (chunkSize % 2 == 1) ? chunkSize + 1 : chunkSize;

			if (chunkID == FMT_CHUNK_ID) {
				// Flag that the format chunk has been found
				foundFormat = true;

				// Read in the header info
				bytesRead = readFormat(file);
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

		if (!foundData) throw new IOException("Did not find a data chunk");

		bufferPointer = 0;
		frameCounter = 0;
		bytesRead = 0;
		int jackFrame = 0;

		// Create a buffer of jack frame size
		float[] sampleBuffer = new float[JACK_BUFFER * STEREO];
		int framesRead;
		do {
			framesRead = readFrames(sampleBuffer, 0, JACK_BUFFER);

			float[][] frame = new float[2][JACK_BUFFER];
			if (numChannels == 2) {
				// stereo -> loop Recording format
				for (int i = 0 ; i < framesRead * STEREO; i += 2) {
					int jack = i / 2;
					frame[LEFT][jack]  = factor * sampleBuffer[i];
					frame[RIGHT][jack] = factor * sampleBuffer[i + 1];
				}
			} else {
				// mono -> duplicate
				for (int i = 0 ; i < framesRead; i++) {
					float v = factor * sampleBuffer[i];
					frame[LEFT][i] = v;
					frame[RIGHT][i] = v;
				}
			}
			into.add(frame);
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
			for (int c = 0 ; c < numChannels ; c++) {
				sampleBuffer[offset] = readSample() / FLOAT_SCALE;
				offset++;
			}
			frameCounter++;
		}
		return numFramesToRead;
	}

	private long readSample() throws IOException {
		long val = 0;
		for (int b = 0 ; b < SAMPLE_BYTES ; b++) {
			if (bufferPointer == bytesRead)  {
				int read = iStream.read(buffer, 0, DISK_BUFFER);
				if (read == -1) throw new IOException("Not enough data available");
				bytesRead = read;
				bufferPointer = 0;
			}
			int v = buffer[bufferPointer];
			if (b < SAMPLE_BYTES - 1 || SAMPLE_BYTES == 1) v &= 0xFF;
			val += (long) v << (b * 8);
			bufferPointer++;
		}
		return val;
	}

	private int readFormat(File file) throws IOException {
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
	 * @return little endian data to int from pos in the supplied buffer */
	static int getLE(byte[] buffer, int pos, int numBytes) {
		numBytes--;
		pos += numBytes;
		int val = buffer[pos] & 0xFF;
		for (int b = 0 ; b < numBytes ; b++)
			val = (val << 8) + (buffer[--pos] & 0xFF);
		return val;
	}

	/** Hard cap for one in‑memory Recording, in bytes. */
	private static final long MAX_RECORDING_BYTES = 256L * 1024L * 1024L; // 256 MB

	/**
	 * Estimate bytes needed for a full in‑memory Recording only
	 * (no FFTs / analysis). Used by Loop, Sample Library, etc.
	 */
	public static boolean canLoadRecording(File f) {
		if (f == null || !f.isFile()) return false;

		long fileBytes = f.length();
		if (fileBytes <= 0) return false;

		try {
			String name = f.getName().toLowerCase();
			boolean compressed = name.endsWith(".mp3")
					|| name.endsWith(".flac")
					|| name.endsWith(".ogg")
					|| name.endsWith(".m4a");

			int sampleRate = S_RATE;
			int channels = 2;
			int bytesPerSample = 2; // 16-bit

			long bytesPerSecondPcm = (long) sampleRate * channels * bytesPerSample;
			if (bytesPerSecondPcm <= 0) return true;

			// Compressed formats: assume ~8:1 compression → more seconds per byte.
			double effectiveBytes = compressed ? fileBytes * 8.0 : fileBytes;

			double seconds = effectiveBytes / bytesPerSecondPcm;
			double fps = FPS; // JACK frames per second
			long frames = (long) Math.ceil(seconds * fps);
			if (frames <= 0) return true;

			// Recording memory: 2 channels * JACK_BUFFER * 4 bytes (float)
			long bytesPerFrame = 2L * JACK_BUFFER * Float.BYTES;
			long recordingBytes = frames * bytesPerFrame;

			long maxHeap = Runtime.getRuntime().maxMemory();
			long softCap = maxHeap / 3;                 // at most 1/3 heap
			long limit = Math.min(softCap, MAX_RECORDING_BYTES);

			return recordingBytes < limit;

		} catch (Throwable t) {
			RTLogger.warn(FromDisk.class, t);
			return true;
		}
	}

	/**
	 * Estimate bytes needed for a Recording plus JudahScope-style FFT analysis
	 * (Transform[] with amplitudes). Use this for Scope to leave room for FFTs.
	 */
	public static boolean canLoadForScope(File f) {
		if (f == null || !f.isFile()) return false;

		long fileBytes = f.length();
		if (fileBytes <= 0) return false;

		try {
			String name = f.getName().toLowerCase();
			boolean compressed = name.endsWith(".mp3")
					|| name.endsWith(".flac")
					|| name.endsWith(".ogg")
					|| name.endsWith(".m4a");

			int sampleRate = S_RATE;
			int channels = 2;
			int bytesPerSample = 2; // 16-bit

			long bytesPerSecondPcm = (long) sampleRate * channels * bytesPerSample;
			if (bytesPerSecondPcm <= 0) return true;

			double effectiveBytes = compressed ? fileBytes * 8.0 : fileBytes;
			double seconds = effectiveBytes / bytesPerSecondPcm;
			double fps = FPS;
			long frames = (long) Math.ceil(seconds * fps);
			if (frames <= 0) return true;

			// Recording memory
			long bytesPerFrame = 2L * JACK_BUFFER * Float.BYTES;
			long recordingBytes = frames * bytesPerFrame;

			// FFT / Transform memory (Scope)
			int chunksPerFft = FFT_SIZE / JACK_BUFFER;        // CHUNKS
			long fftFrames = Math.max(1, frames / Math.max(1, chunksPerFft));
			long amplitudesPerFrame = FFT_SIZE / 2;           // AMPLITUDES
			long fftBytes = fftFrames * amplitudesPerFrame * Float.BYTES;

			long estimatedBytes = recordingBytes + fftBytes;

			long maxHeap = Runtime.getRuntime().maxMemory();
			long softCap = maxHeap / 3;                       // at most 1/3 heap
			long limit = Math.min(softCap, MAX_RECORDING_BYTES);

			return estimatedBytes < limit;

		} catch (Throwable t) {
			RTLogger.warn(FromDisk.class, t);
			return true;
		}
	}
}