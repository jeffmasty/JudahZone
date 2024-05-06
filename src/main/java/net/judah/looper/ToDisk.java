package net.judah.looper;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.FloatBuffer;
import java.util.Date;
import java.util.concurrent.LinkedBlockingQueue;

import net.judah.JudahZone;
import net.judah.util.Constants;
import net.judah.util.Memory;
import net.judah.util.RTLogger;

/** Save a loop or live audio to disk. <br/>
 * <br/><br/><pre>
	Wav file IO class
 	A.Greensted
	http://www.labbookpages.co.uk/audio/javaWavFiles.html

	File format is based on the information from
	http://www.sonicspot.com/guide/wavefiles.html
	http://www.blitter.com/~russtopia/MIDI/~jglatt/tech/wave.htm </pre>*/
public class ToDisk extends LinkedBlockingQueue<float[][]> implements Closeable, Runnable, WavConstants {

	private final Memory memory = new Memory(2, JACK_SIZE);
	private RandomAccessFile oStream;	// Output stream used for writing data
	private File target;
	public boolean active = true;
	private int jackFrames;
	private int numFrames;
	private int dataChunkSize;
	private int mainChunkSize;
	// Buffering
	private byte[] buffer = new byte[BUFFER_SIZE];
	protected int bufferPointer;		// Points to the current position in local buffer
	protected boolean wordAlignAdjust;	// Specify if an extra byte at the end of the data chunk is required for word alignment
	
	@SuppressWarnings("deprecation")
	public ToDisk() throws IOException {
		Date d = new Date();
		String name = (d.getYear() + 1900) + "-" + d.getMonth() + "-" + d.getDate() 
				+ "." + d.getHours() + "h" + d.getMinutes() + "m" + d.getSeconds() + "s";
		target = new File(System.getProperty("user.home"), name + ".wav");
		init(true);
	}
	
	public ToDisk(File f) throws IOException {
		if (!f.getName().endsWith(".wav"))
			f = new File(f.getParent(), f.getName() + ".wav");
		target = f;
		init(true);
	}
	
	public static void toDisk(Recording rec, File f, int frameCount) throws IOException {
		new ToDisk(rec, f, frameCount);
	}
	private ToDisk(Recording rec, File f, int frameCount) throws IOException {
		if (f == null) 
			return;
		if (rec == null || frameCount == 0) {
			RTLogger.log(this, "no recording");
			return;
		}

		this.jackFrames = frameCount;
		if (!f.getName().endsWith(".wav"))
			f = new File(f.getParent(), f.getName() + ".wav");
		target = f;
		init(false);
		for (int frame = 0; frame < rec.size(); frame++) 
			writeFrame(rec.get(frame));
		if (bufferPointer > 0) oStream.write(buffer, 0, bufferPointer);
		// If an extra byte is required for word alignment, add it to the end
		if (wordAlignAdjust) 
			oStream.write(0);
		// Close the stream and set to null
		oStream.close();
		oStream = null;
		RTLogger.log(this, "saved " + target.getAbsolutePath());
	}
	
	private void init(boolean threaded) throws IOException {
		oStream = new RandomAccessFile(target, "rw");
		calc();
		writeHeader();
		if (threaded) {
			JudahZone.getServices().add(this);
			active = true;
			new Thread(this).start();
		}
	}

	private void calc() {
		numFrames = jackFrames == 0 ? 1 /*tmp*/ : jackFrames * JACK_SIZE; 
		// Calculate the chunk sizes
		dataChunkSize = BLOCK_ALIGN * numFrames;
		mainChunkSize =	4 +	// Riff Type
						8 +	// Format ID and size
						16 +	// Format data
						8 + 	// Data ID and size
						dataChunkSize;
		// Chunks must be word aligned, so if odd number of audio data bytes
		// adjust the main chunk size
		if (dataChunkSize % 2 == 1) {
			mainChunkSize += 1;
			wordAlignAdjust = true;
		} else {
			wordAlignAdjust = false;
		}
	}

	public void offer(FloatBuffer left, FloatBuffer right) {
		float[][] data = memory.getFrame();
		left.rewind(); right.rewind();
		for (int i = 0; i < JACK_SIZE; i++) {
			data[LEFT][i] = left.get();
			data[RIGHT][i] = right.get();
		}
		offer(data);
	}

	@Override public void close() throws IOException {
		active = false;
		// Write out anything still in the local buffer
		if (bufferPointer > 0) oStream.write(buffer, 0, bufferPointer);
		calc();
		// If an extra byte is required for word alignment, add it to the end
		if (wordAlignAdjust) 
			oStream.write(0);
		// re-write header with total audio length
		writeHeader();
		// Close the stream and set to null
		oStream.close();
		oStream = null;
		JudahZone.getServices().remove(this);
		JudahZone.getMidiGui().updateTape();
		RTLogger.log(this, target.getAbsolutePath());
	}

	@Override public void run() {
		JudahZone.getMidiGui().updateTape();
		try {
			while (active) {
				writeFrame(take());
				jackFrames++;
			}
		} catch (Throwable t) { RTLogger.warn(this, t); }
	}
	
	private void writeFrame(float[][] sampleBuffer) throws IOException {
		for (int i = 0; i < JACK_SIZE; i++) {
			writeSample((long)(FLOAT_SCALE * sampleBuffer[LEFT][i]));
			writeSample((long)(FLOAT_SCALE * sampleBuffer[RIGHT][i]));
		}
	}

	private void writeSample(long val) throws IOException {
		for (int b = 0 ; b < SAMPLE_BYTES ; b++) {
			if (bufferPointer == BUFFER_SIZE) {
				oStream.write(buffer, 0, BUFFER_SIZE);
				bufferPointer = 0;
			}
			buffer[bufferPointer] = (byte) (val & 0xFF);
			val >>= 8;
			bufferPointer ++;
		}
	}

	public void writeHeader() throws IOException {
		oStream.seek(0);
		byte[] buffer = new byte[4096]; // local buffer for disk IO		

		// Set the main chunk size
		putLE(Recording.RIFF_CHUNK_ID,	buffer, 0, 4);
		putLE(mainChunkSize,	buffer, 4, 4);
		putLE(Recording.RIFF_TYPE_ID,	buffer, 8, 4);

		// Write out the header
		oStream.write(buffer, 0, 12);

		// Put format data in buffer
		int averageBytesPerSecond = S_RATE * BLOCK_ALIGN;

		putLE(FMT_CHUNK_ID,				buffer, 0, 4);		// Chunk ID
		putLE(16,						buffer, 4, 4);		// Chunk Data Size
		putLE(1,						buffer, 8, 2);		// Compression Code (Uncompressed)
		putLE(STEREO,				buffer, 10, 2);			// Number of channels
		putLE(S_RATE,				buffer, 12, 4);			// Sample Rate
		putLE(averageBytesPerSecond,	buffer, 16, 4);		// Average Bytes Per Second
		putLE(BLOCK_ALIGN,		        buffer, 20, 2);		// Block Align
		putLE(VALID_BITS,				buffer, 22, 2);		// Valid Bits

		// Write Format Chunk
		oStream.write(buffer, 0, 24);

		// Start Data Chunk
		putLE(DATA_CHUNK_ID,			buffer, 0, 4);		// Chunk ID
		putLE(dataChunkSize,			buffer, 4, 4);		// Chunk Data Size

		// Write Format Chunk
		oStream.write(buffer, 0, 8);

		// set the IO State
		bufferPointer = 0;
	}

	/**put val as little endian at pos in supplied buffer*/
	public static void putLE(int val, byte[] buffer, int pos, int numBytes) {
		for (int b=0 ; b<numBytes ; b++) {
			buffer[pos] = (byte) (val & 0xFF);
			val >>= 8;
			pos ++;
		}
	}

	@Override public float seconds() {
		return jackFrames / Constants.fps();
	}
	
}
