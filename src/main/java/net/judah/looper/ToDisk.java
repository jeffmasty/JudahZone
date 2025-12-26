package net.judah.looper;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.FloatBuffer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.LinkedBlockingQueue;

import net.judah.JudahZone;
import net.judah.omni.Recording;
import net.judah.omni.WavConstants;
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

	private final Memory memory = new Memory(2, JACK_BUFFER);
	private RandomAccessFile oStream;	// Output stream used for writing data
	private File target;
	public boolean active = true;
	private int jackFrames;
	private int numFrames;
	private int dataChunkSize;
	private int mainChunkSize;
	// Buffering
	private byte[] buffer = new byte[DISK_BUFFER];
	protected int bufferPointer;		// Points to the current position in local buffer
	protected boolean wordAlignAdjust;	// Specify if an extra byte at the end of the data chunk is required for word alignment

	// Track exactly how many audio bytes have been written to the data chunk
	// This prevents mismatch between header sizes and actual audio payload.
	private long totalDataBytes = 0L;

	public ToDisk() throws IOException {
		LocalDateTime now = LocalDateTime.now();
		DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd.HH'h'mm'm'ss's'");
		String name = now.format(fmt);
		target = new File(System.getProperty("user.home"), name + ".wav");
		init(true);
	}

	// TODO mastering?
	public ToDisk(File f) throws IOException {
		if (!f.getName().endsWith(".wav"))
			f = new File(f.getParent(), f.getName() + ".wav");
		target = f;
		init(true);
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
		numFrames = jackFrames == 0 ? 1 /*tmp*/ : jackFrames * JACK_BUFFER;
		// Calculate the chunk sizes
		dataChunkSize = BLOCK_ALIGN * numFrames;
		// Chunks must be word aligned, so if odd number of audio data bytes
		// adjust the main chunk size
		if (dataChunkSize % 2 == 1) {
			dataChunkSize += 1;
			wordAlignAdjust = true;
		} else
			wordAlignAdjust = false;

		// RIFF main chunk size = 36 + dataChunkSize (4 "WAVE" + (8+16) fmt chunk + (8) data header)
		mainChunkSize = 36 + dataChunkSize;
	}

	public void offer(FloatBuffer left, FloatBuffer right) {
		float[][] data = memory.getFrame();
		left.rewind(); right.rewind();
		for (int i = 0; i < JACK_BUFFER; i++) {
			data[LEFT][i] = left.get();
			data[RIGHT][i] = right.get();
		}
		offer(data);
	}

	@Override public void close() throws IOException {
		active = false;
		// Write out anything still in the local buffer
		if (bufferPointer > 0) oStream.write(buffer, 0, bufferPointer);

		// Recalculate data sizes using the actual bytes written
		// totalDataBytes counts only audio payload bytes; convert to int for WAV header (WAV uses 32-bit sizes).
		dataChunkSize = (int) totalDataBytes;

		// Chunks must be word aligned
		if (dataChunkSize % 2 == 1) {
			dataChunkSize += 1;
			wordAlignAdjust = true;
		} else {
			wordAlignAdjust = false;
		}

		// If an extra byte is required for word alignment, add it to the end
		if (wordAlignAdjust)
			oStream.write(0);

		// Recompute main chunk size from actual data chunk size and rewrite header
		mainChunkSize = 36 + dataChunkSize;
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
		for (int i = 0; i < JACK_BUFFER; i++) {
			writeSample((long)(FLOAT_SCALE * sampleBuffer[LEFT][i]));
			writeSample((long)(FLOAT_SCALE * sampleBuffer[RIGHT][i]));
		}
	}

	private void writeSample(long val) throws IOException {
		for (int b = 0 ; b < SAMPLE_BYTES ; b++) {
			if (bufferPointer == DISK_BUFFER) {
				oStream.write(buffer, 0, DISK_BUFFER);
				bufferPointer = 0;
			}
			buffer[bufferPointer] = (byte) (val & 0xFF);
			val >>= 8;
			bufferPointer ++;
		}
		// Account for the written audio bytes (each sample is SAMPLE_BYTES bytes)
		totalDataBytes += SAMPLE_BYTES;
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

}

// TODO:
/*
class StemWriter {
  RandomAccessFile out;
  byte[] buf = new byte[DISK_BUFFER];
  int bufPtr = 0;
  long totalDataBytes = 0;
  int dataChunkSize, mainChunkSize;
  boolean wordAlignAdjust;
  boolean stereo; // false = mono, true = stereo
  int sampleBytes; // 2 for s16, 4 for float

  StemWriter(File target, boolean stereo, int sampleBytes) { ... writeHeader(); }

  void writeSampleBytes(byte[] sampleLE) throws IOException {
    // copy sampleLE to buf, flush if needed
    System.arraycopy(sampleLE, 0, buf, bufPtr, sampleLE.length);
    bufPtr += sampleLE.length;
    if (bufPtr >= buf.length) { out.write(buf,0,bufPtr); bufPtr=0; }
    totalDataBytes += sampleLE.length;
  }

  // call per-frame with floats: for mono write L value, for stereo write L then R
  void writeFrame(float[] frame) { for(...) convert & writeSampleBytes(...); }

  void close() {
    if (bufPtr>0) out.write(buf,0,bufPtr);
    dataChunkSize = (int)totalDataBytes;
    if (dataChunkSize %2 ==1) { dataChunkSize++; wordAlignAdjust=true; out.write(0); }
    mainChunkSize = 36 + dataChunkSize;
    writeHeader(); // seek(0) rewrite header with sizes
    out.close();
  }

  // writeHeader similar to ToDisk.writeHeader but uses this.dataChunkSize etc
}*/