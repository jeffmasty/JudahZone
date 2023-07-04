package net.judah.looper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Vector;

import lombok.Getter;
import lombok.NoArgsConstructor;
import net.judah.util.AudioTools;
import net.judah.util.Constants;

/**	------------WavFile------------<br/>
 * Audio data for a Loop or Sample, blank for recording or load .wav file from disk. <br/>
 * <br/><br/><pre>
	Wav file IO class
 	A.Greensted
	http://www.labbookpages.co.uk/audio/javaWavFiles.html

	File format is based on the information from
	http://www.sonicspot.com/guide/wavefiles.html
	http://www.blitter.com/~russtopia/MIDI/~jglatt/tech/wave.htm </pre>*/
@NoArgsConstructor
public class Recording extends Vector<float[][]> implements WavConstants {
	public static final float BOOST = 5f;

	@Getter private long creationTime = System.currentTimeMillis();
	@Getter private File file; // can be null (looper)				
	private FileInputStream iStream;	// Input stream used for reading data
	private final byte[] buffer = new byte[BUFFER_SIZE]; // local buffer for disk IO
	private int numFrames;		// Number of frames within the data section
	int numChannels = 2;	// 2 bytes unsigned, 0x0001 (1) to 0xFFFF (65,535)
	boolean stereo = true;
	long blockAlign;		// 2 bytes unsigned, 0x0001 (1) to 0xFFFF (65,535)
	private int bufferPointer;			// Points to the current position in local buffer
	private int bytesRead;				// Bytes read after last read into local buffer
	private int frameCounter;			// Current number of frames read or written
	
	public Recording(Recording recording, int duplications) {
		int size = recording.size() * duplications;
		int x = 0;
		for (int j = 0; j < size; j++) {
			x++;
			if (x >= recording.size())
				x = 0;
			float[][] data = new float[2][JACK_SIZE];
			AudioTools.copy(recording.get(x), data);
			add(data);
		}
	}
	
	public Recording (File file, float factor) throws Exception {
		load(file, factor);
	}
	
	public Recording (File file) throws Exception {
		load(file, BOOST);
	}
	
	/**@return size of file in frames */
	public int load(File file, float factor) throws Exception {
		final int size = size();
		this.file = file;
		iStream = new FileInputStream(file);

		// Read the first 12 bytes of the file
		int bytesRead = iStream.read(buffer, 0, 12);
		if (bytesRead != 12) throw new Exception("Not enough wav file bytes for header");

		// Extract parts from the header
		long riffChunkID = getLE(buffer, 0, 4);
		int chunkSize = getLE(buffer, 4, 4);
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
		while (true) {
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
				if (!foundFormat) throw new Exception("Data chunk found before Format chunk");

				// Check that the chunkSize (wav data length) is a multiple of the
				// block align (bytes per frame)
				if (chunkSize % blockAlign != 0) throw new Exception("Data Chunk size is not multiple of Block Align");

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

		if (foundData == false) throw new Exception("Did not find a data chunk");
		bufferPointer = 0;
		frameCounter = 0;
		bytesRead = 0;
		int jackFrame = 0;
		stereo = numChannels == 2;
		// Create a buffer of jack frame size
		float[] buffer = new float[JACK_SIZE * STEREO];
		int framesRead;
		do {
            // Read frames into buffer
            framesRead = readFrames(buffer, 0, JACK_SIZE);

            float[][] frame = new float[2][JACK_SIZE]; 
            if (stereo)
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

	private int readFrames(float[] sampleBuffer, int offset, int numFramesToRead) throws IOException, Exception {
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
	
	private long readSample() throws Exception {
		long val = 0;
		for (int b=0 ; b<SAMPLE_BYTES ; b++) {
			if (bufferPointer == bytesRead)  {
				int read = iStream.read(buffer, 0, BUFFER_SIZE);
				if (read == -1) throw new Exception("Not enough data available");
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
			throw new IOException("Bit Depth(" + VALID_BITS + ") vs: " + bits + " " + file.getAbsolutePath());
		if (numChannels == 0) throw new IOException("Number of channels specified in header is equal to zero");
		if (blockAlign == 0) throw new IOException("Block Align specified in header is equal to zero");
		if (SAMPLE_BYTES * numChannels != blockAlign)
			throw new IOException("Block Align does not agree with bytes required for VALID_BITS and number of channels");
		return result;
	}
	
	/**@param supplied buffer
	 * @param pos
	 * @param numBytes
	 * @return little endiean data to long from pos in the supplied buffer */
	public static int getLE(byte[] buffer, int pos, int numBytes) {
		numBytes --;
		pos += numBytes;
		int val = buffer[pos] & 0xFF;
		for (int b=0 ; b<numBytes ; b++) 
			val = (val << 8) + (buffer[--pos] & 0xFF);
		return val;
	}

	@Override public float seconds() {
		return size() / Constants.fps();
	}

}
