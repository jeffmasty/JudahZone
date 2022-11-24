package net.judah.looper;

import static net.judah.util.Constants.getLE;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Vector;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import lombok.Getter;
import net.judah.util.AudioTools;
import net.judah.util.Constants;

/** Actual audio data of a Loop or Sample, with the ability to overdub off the RT thread and load from disk. <br/>
 * <br/><br/><pre>
	Wav file IO class
 	A.Greensted
	http://www.labbookpages.co.uk/audio/javaWavFiles.html

	File format is based on the information from
	http://www.sonicspot.com/guide/wavefiles.html
	http://www.blitter.com/~russtopia/MIDI/~jglatt/tech/wave.htm </pre>*/
public class Recording extends Vector<float[][]> {
	@Getter private long creationTime = System.currentTimeMillis();
	@Getter private File file; // can be null				
	private BlockingQueue<float[][]> newQueue;
	private BlockingQueue<float[][]> oldQueue;
	private BlockingQueue<Integer> locationQueue;
	private class Runner extends Thread {
		@Override public void run() {
			try {
			while (true) { // MIX
				set(locationQueue.take(), AudioTools.overdub(newQueue.take(), oldQueue.take()));
			}} catch (InterruptedException e) {  }}};
	private Runner runner;
	// ------------WavFile --------------------------
	private byte[] buffer = new byte[4096]; // local buffer for disk IO
	@Getter private static final int validBits = 16;		// 2 bytes unsigned, 0x0002 (2) to 0xFFFF (65,535)
	private static final int frameSize = Constants.bufSize();
	private static final int LEFT = Constants.LEFT_CHANNEL;
	private static final int RIGHT = Constants.RIGHT_CHANNEL;
	private static final int STEREO = Constants.STEREO;
	
	private enum IOState {READING, WRITING, CLOSED};
	private static final int BUFFER_SIZE = 4096;
	
	private static final int FMT_CHUNK_ID = 0x20746D66;
	private static final int DATA_CHUNK_ID = 0x61746164;
	private static final int RIFF_CHUNK_ID = 0x46464952;
	private static final int RIFF_TYPE_ID = 0x45564157;
	
	private IOState ioState;			// Specifies the IO State of the Wav File (used for snaity checking)
	private int bytesPerSample;			// Number of bytes required to store a single sample
	@Getter private long numFrames;		// Number of frames within the data section
	//private FileOutputStream oStream;	// Output stream used for writting data
	private FileInputStream iStream;	// Input stream used for reading data
	private double floatScale;			// Scaling factor used for int <-> float conversion				
	private double floatOffset;			// Offset factor used for int <-> float conversion				
	//private boolean wordAlignAdjust;	// Specify if an extra byte at the end of the data chunk is required for word alignment
	// Wav Header
	@Getter int numChannels;	// 2 bytes unsigned, 0x0001 (1) to 0xFFFF (65,535)
	@Getter boolean stereo;
	// 4 bytes unsigned, 0x00000001 (1) to 0xFFFFFFFF (4,294,967,295)
	// Although a java int is 4 bytes, it is signed, so need to use a long
	@Getter int blockAlign;		// 2 bytes unsigned, 0x0001 (1) to 0xFFFF (65,535)
	// Buffering
	private int bufferPointer;			// Points to the current position in local buffer
	private int bytesRead;				// Bytes read after last read into local buffer
	private long frameCounter;			// Current number of frames read or written

	public Recording(int size) {
		this(size, true);
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
	}

	/** create empty recording of size */
	public Recording(int size, boolean startListeners) {
		this(startListeners);
		int bufferSize = Constants.bufSize();
		for (int j = 0; j < size; j++) {
			float[][] data = new float[2][bufferSize];
			add(data);
		}
	}

	public Recording(Recording recording, int duplications, boolean startListeners) {
		this(startListeners);
		int bufferSize = Constants.bufSize();
		int size = recording.size() * duplications;
		int x = 0;
		for (int j = 0; j < size; j++) {
			x++;
			if (x >= recording.size())
				x = 0;
			float[][] data = new float[2][bufferSize];
			AudioTools.copy(recording.get(x), data);
			add(data);
		}
	}

	public Recording (File file) throws Exception {
		this(0, false);
		this.file = file;
		iStream = new FileInputStream(file);

		// Read the first 12 bytes of the file
		int bytesRead = iStream.read(buffer, 0, 12);
		if (bytesRead != 12) throw new Exception("Not enough wav file bytes for header");

		// Extract parts from the header
		long riffChunkID = getLE(buffer, 0, 4);
		long chunkSize = getLE(buffer, 4, 4);
		long riffTypeID = getLE(buffer, 8, 4);

		// Check the header bytes contains the correct signature
		if (riffChunkID != RIFF_CHUNK_ID) throw new Exception("Invalid Wav Header data, incorrect riff chunk ID");
		if (riffTypeID != RIFF_TYPE_ID) throw new Exception("Invalid Wav Header data, incorrect riff type ID");

		// Check that the file size matches the number of bytes listed in header
		if (file.length() != chunkSize+8) {
			throw new Exception("Header chunk size (" + chunkSize + ") does not match file size (" + file.length() + ")");
		}

		boolean foundFormat = false;
		boolean foundData = false;

		// Search for the Format and Data Chunks
		while (true)
		{
			// Read the first 8 bytes of the chunk (ID and chunk size)
			bytesRead = iStream.read(buffer, 0, 8);
			if (bytesRead == -1) throw new Exception("Reached end of file without finding format chunk");
			if (bytesRead != 8) throw new Exception("Could not read chunk header");

			// Extract the chunk ID and Size
			long chunkID = getLE(buffer, 0, 4);
			chunkSize = getLE(buffer, 4, 4);

			// Word align the chunk size
			// chunkSize specifies the number of bytes holding data. However,
			// the data should be word aligned (2 bytes) so we need to calculate
			// the actual number of bytes in the chunk
			long numChunkBytes = (chunkSize%2 == 1) ? chunkSize+1 : chunkSize;

			if (chunkID == FMT_CHUNK_ID)
			{
				// Flag that the format chunk has been found
				foundFormat = true;

				// Read in the header info
				bytesRead = iStream.read(buffer, 0, 16);

				// Check this is uncompressed data
				int compressionCode = (int) getLE(buffer, 0, 2);
				if (compressionCode != 1) throw new Exception("Compression Code " + compressionCode + " not supported");

				// Extract the format information
				numChannels = (int) getLE(buffer, 2, 2);
				long srate = getLE(buffer, 4, 4);
				if (srate != Constants.sampleRate()) 
					throw new Exception("Sample rate(" + Constants.sampleRate() + ") vs: " + srate + " " + file.getAbsolutePath());
				
				// sampleRate = getLE(buffer, 4, 4);
				blockAlign = (int) getLE(buffer, 12, 2);
				
				long bits = getLE(buffer, 14, 2);
				if (bits != validBits)
					throw new Exception("Bit Depth(" + validBits + ") vs: " + bits + " " + file.getAbsolutePath());
				
				if (numChannels == 0) throw new Exception("Number of channels specified in header is equal to zero");
				if (blockAlign == 0) throw new Exception("Block Align specified in header is equal to zero");

				// Calculate the number of bytes required to hold 1 sample
				bytesPerSample = (validBits + 7) / 8;
				if (bytesPerSample * numChannels != blockAlign)
					throw new Exception("Block Align does not agree with bytes required for validBits and number of channels");

				// Account for number of format bytes and then skip over
				// any extra format bytes
				numChunkBytes -= 16;
				if (numChunkBytes > 0) iStream.skip(numChunkBytes);
			}
			else if (chunkID == DATA_CHUNK_ID)
			{
				// Check if we've found the format chunk,
				// If not, throw an exception as we need the format information
				// before we can read the data chunk
				if (foundFormat == false) throw new Exception("Data chunk found before Format chunk");

				// Check that the chunkSize (wav data length) is a multiple of the
				// block align (bytes per frame)
				if (chunkSize % blockAlign != 0) throw new Exception("Data Chunk size is not multiple of Block Align");

				// Calculate the number of frames
				numFrames = chunkSize / blockAlign;
				
				// Flag that we've found the wave data chunk
				foundData = true;

				break;
			}
			else
			{
				// If an unknown chunk ID is found, just skip over the chunk data
				iStream.skip(numChunkBytes);
			}
		
		}
		// Throw an exception if no data chunk has been found
		if (foundData == false) throw new Exception("Did not find a data chunk");

		// Calculate the scaling factor for converting to a normalised double 
		// If more than 8 validBits, data is signed
		// Conversion required dividing by magnitude of max negative value
		floatOffset = 0;
		floatScale = 1 << (validBits - 1);

		bufferPointer = 0;
		bytesRead = 0;
		frameCounter = 0;
		ioState = IOState.READING;

		stereo = getNumChannels() == 2;
		// Create a buffer of jack frame size
		double[] buffer = new double[frameSize * STEREO];

		int framesRead;
		do {
            // Read frames into buffer
            framesRead = readFrames(buffer, 0, frameSize);

            float[][] frame = new float[2][frameSize]; 
            if (stereo)
	            // cycle through frames and put them in loop Recording format
	            for (int i = 0 ; i < framesRead * STEREO; i += 2) {
	            	frame[LEFT][i/2] = (float)buffer[i];
	            	frame[RIGHT][i/2] = (float)buffer[i + 1];
	            }
            else 
	            for (int i = 0 ; i < framesRead; i++) {
	            	frame[LEFT][i] = (float)buffer[i];
	            	frame[RIGHT][i] = (float)buffer[i];
	            }
            add(frame);
		} while (framesRead != 0);
		
		if (iStream != null) {
			iStream.close();
			iStream = null;
		}
	}

	/** throw away first frame (and last?) */
	public void trim() { // minor discrepancy vs. midi clock
		remove(0);
	}
	
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

	private long readSample() throws Exception {
		long val = 0;

		for (int b=0 ; b<bytesPerSample ; b++) {
			if (bufferPointer == bytesRead)  {
				int read = iStream.read(buffer, 0, BUFFER_SIZE);
				if (read == -1) throw new Exception("Not enough data available");
				bytesRead = read;
				bufferPointer = 0;
			}

			int v = buffer[bufferPointer];
			if (b < bytesPerSample-1 || bytesPerSample == 1) v &= 0xFF;
			val += v << (b * 8);

			bufferPointer ++;
		}

		return val;
	}
	
	public int readFrames(double[] sampleBuffer, int offset, int numFramesToRead) throws IOException, Exception {
		if (ioState != IOState.READING) throw new IOException("Cannot read from WavFile instance");

		for (int f=0 ; f<numFramesToRead ; f++) {
			if (frameCounter == numFrames) return f;

			for (int c=0 ; c<numChannels ; c++) {
				sampleBuffer[offset] = floatOffset + readSample() / floatScale;
				offset ++;
			}

			frameCounter ++;
		}

		return numFramesToRead;
	}

	public void close() {
		if (runner != null) {
			runner.interrupt();
			runner = null;
		}
	}
	

}
