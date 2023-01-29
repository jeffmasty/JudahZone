package net.judah.looper;
// Wav file IO class
// A.Greensted
// http://www.labbookpages.co.uk/audio/javaWavFiles.html
//
// File format is based on the information from
// http://www.sonicspot.com/guide/wavefiles.html
// http://www.blitter.com/~russtopia/MIDI/~jglatt/tech/wave.htm

import static net.judah.util.Constants.putLE;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.swing.SwingUtilities;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.judah.gui.widgets.FileChooser;
import net.judah.util.Constants;
import net.judah.util.RTLogger;

@RequiredArgsConstructor
public class WavTools extends MouseAdapter {
	
	private static final long sampleRate = Constants.sampleRate();	
	private static final int validBits = 16;		// 2 bytes unsigned, 0x0002 (2) to 0xFFFF (65,535)
	private static final int BUFFER_SIZE = 4096;
	private final byte[] buffer = new byte[BUFFER_SIZE];
	
	private static final int frameSize = Constants.bufSize();
	private static final int bufSize = Constants.bufSize();
	private static final int LEFT = Constants.LEFT_CHANNEL;
	private static final int RIGHT = Constants.RIGHT_CHANNEL;
	private static final int STEREO = Constants.STEREO;
	
	private enum IOState {READING, WRITING, CLOSED};
	
	private static final int FMT_CHUNK_ID = 0x20746D66;
	private static final int DATA_CHUNK_ID = 0x61746164;
	private static final int RIFF_CHUNK_ID = 0x46464952;
	private static final int RIFF_TYPE_ID = 0x45564157;
	
	private final Loop loop;
	private IOState ioState;			// Specifies the IO State of the Wav File (used for snaity checking)
	private int bytesPerSample;			// Number of bytes required to store a single sample
	@Getter private long numFrames;		// Number of frames within the data section
	private FileOutputStream oStream;	// Output stream used for writting data
	private FileInputStream iStream;	// Input stream used for reading data
	private double floatScale;			// Scaling factor used for int <-> float conversion				
	private double floatOffset;			// Offset factor used for int <-> float conversion				
	private boolean wordAlignAdjust;	// Specify if an extra byte at the end of the data chunk is required for word alignment

	// Wav Header
	@Getter int numChannels = 2;	// 2 bytes unsigned, 0x0001 (1) to 0xFFFF (65,535)
	@Getter boolean stereo = true;
	// 4 bytes unsigned, 0x00000001 (1) to 0xFFFFFFFF (4,294,967,295)
	// Although a java int is 4 bytes, it is signed, so need to use a long
	@Getter int blockAlign;		// 2 bytes unsigned, 0x0001 (1) to 0xFFFF (65,535)

	// Buffering
	private int bufferPointer;			// Points to the current position in local buffer
	private int bytesRead;				// Bytes read after last read into local buffer
	private long frameCounter;			// Current number of frames read or written

			
	@Override public void mouseClicked(MouseEvent ev) {
		if (SwingUtilities.isRightMouseButton(ev)) {
			if (loop.hasRecording()) {
				try {
					save();
				} catch (Exception e) {
					RTLogger.warn(loop.getRecording(), e);
				}
			}
		}
	}
	
	
	

	public void blankWavFile(File file, final long numFrames) 
			throws Exception {
		
		byte[] buffer = new byte[4096]; // local buffer for disk IO		
//		loop.numChannels = 2; // zone recordings are stereo
		bytesPerSample = (validBits + 7) / 8;
		blockAlign = bytesPerSample * numChannels;
		this.numFrames = numFrames;
		
		// Sanity check arguments
		if (numChannels < 1 || numChannels > 65535) throw new Exception("Illegal number of channels, valid range 1 to 65536");
		if (numFrames < 0) throw new Exception("Number of frames must be positive");

		// Create output stream for writing data
		oStream = new FileOutputStream(file);

		// Calculate the chunk sizes
		long dataChunkSize = blockAlign * numFrames;
		long mainChunkSize =	4 +	// Riff Type
									8 +	// Format ID and size
									16 +	// Format data
									8 + 	// Data ID and size
									dataChunkSize;

		// Chunks must be word aligned, so if odd number of audio data bytes
		// adjust the main chunk size
		if (dataChunkSize % 2 == 1) {
			mainChunkSize += 1;
			wordAlignAdjust = true;
		}
		else {
			wordAlignAdjust = false;
		}

		// Set the main chunk size
		putLE(RIFF_CHUNK_ID,	buffer, 0, 4);
		putLE(mainChunkSize,	buffer, 4, 4);
		putLE(RIFF_TYPE_ID,	buffer, 8, 4);

		// Write out the header
		oStream.write(buffer, 0, 12);

		// Put format data in buffer
		long averageBytesPerSecond = sampleRate * blockAlign;

		putLE(FMT_CHUNK_ID,				buffer, 0, 4);		// Chunk ID
		putLE(16,						buffer, 4, 4);		// Chunk Data Size
		putLE(1,						buffer, 8, 2);		// Compression Code (Uncompressed)
		putLE(numChannels,				buffer, 10, 2);		// Number of channels
		putLE(sampleRate,				buffer, 12, 4);		// Sample Rate
		putLE(averageBytesPerSecond,	buffer, 16, 4);		// Average Bytes Per Second
		putLE(blockAlign,		        buffer, 20, 2);		// Block Align
		putLE(validBits,				buffer, 22, 2);		// Valid Bits

		// Write Format Chunk
		oStream.write(buffer, 0, 24);

		// Start Data Chunk
		putLE(DATA_CHUNK_ID,			buffer, 0, 4);		// Chunk ID
		putLE(dataChunkSize,			buffer, 4, 4);		// Chunk Data Size

		// Write Format Chunk
		oStream.write(buffer, 0, 8);

		// Calculate the scaling factor for converting to a normalised double
		// If more than 8 validBits, data is signed
		// Conversion required multiplying by magnitude of max positive value
		floatOffset = 0;
		floatScale = Long.MAX_VALUE >> (64 - validBits);

		// set the IO State
		bufferPointer = 0;
		bytesRead = 0;
		frameCounter = 0;
		ioState = IOState.WRITING;
	}

	public void save2() throws Exception {
		FileChooser.setCurrentDir(new File(System.getProperty("user.home")));
		File f = FileChooser.choose();
		if (f == null) return;

		int sampleRate = Constants.sampleRate();
		double duration = 5.0;     // Seconds

		// Calculate the number of frames required for specified duration
		long numFrames = (long)(duration * sampleRate);

		blankWavFile(f, numFrames);

		
// Create a buffer of 100 frames
		double[][] buffer = new double[2][100];
         
		// Initialise a local frame counter
		long frameCounter = 0;
		
//		// Loop until all frames written
//		while (frameCounter < numFrames)
//		{
//			// Determine how many frames to write, up to a maximum of the buffer size
//            long remaining = wavFile.getFramesRemaining();
//            int toWrite = (remaining > 100) ? 100 : (int) remaining;
//
//            // Fill the buffer, one tone per channel
//            for (int s=0 ; s<toWrite ; s++, frameCounter++)
//            {
//               buffer[0][s] = Math.sin(2.0 * Math.PI * 400 * frameCounter / sampleRate);
//               buffer[1][s] = Math.sin(2.0 * Math.PI * 500 * frameCounter / sampleRate);
//            }
//
//            // Write the buffer
//            wavFile.writeFrames(buffer, toWrite);
//         }
//
//         // Close the wavFile
//         wavFile.close();		
	}
	
	
	public void save() throws Exception {
		FileChooser.setCurrentDir(new File(System.getProperty("user.home")));
		File f = FileChooser.choose();
		if (f == null) return;
		
		blankWavFile(f, loop.getRecording().getNumFrames());
        if (ioState != IOState.WRITING) throw new IOException("Cannot write to WavFile instance"); 
         
		for (float[][] frame : loop.getRecording()) {
         	float[] left = frame[0];
			float[] right = frame[1];
			for (int i = 0; i < left.length; i++) {
				writeSample((long) (floatScale * (floatOffset + left[i])));
				writeSample((long) (floatScale * (floatOffset + right[i])));
			}
         }
         // Close the wavFile
		if (oStream != null)  {
			// Write out anything still in the local buffer
			if (bufferPointer > 0) oStream.write(buffer, 0, bufferPointer);
	
			// If an extra byte is required for word alignment, add it to the end
			if (wordAlignAdjust) oStream.write(0);
			
			// Close the stream and set to null
			oStream.close();
			oStream = null;
		}
	}
	
		// Sample Writing and Reading
	// --------------------------
	private void writeSample(long val) throws IOException {
		for (int b=0 ; b<bytesPerSample ; b++)
		{
			if (bufferPointer == BUFFER_SIZE) {
				oStream.write(buffer, 0, BUFFER_SIZE);
				bufferPointer = 0;
			}
			buffer[bufferPointer] = (byte) (val & 0xFF);
			val >>= 8;
			bufferPointer ++;
		}
	}


	
}
