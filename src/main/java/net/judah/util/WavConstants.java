package net.judah.util;
// Wav file IO class
// A.Greensted
// http://www.labbookpages.co.uk

public interface WavConstants {

	int S_RATE = 48000; // TODO generalize
    float NYQUIST = S_RATE / 2f;

	/** Samples in a process call */ //TODO upgrade latency to 256!
	int JACK_BUFFER = 512;
	int FFT_SIZE = 4096;
	float FPS = S_RATE / (float)JACK_BUFFER;

	int DISK_BUFFER = 4096; //  read/write
	/** 4 frames for frequency discernment */
//	int FFT_BUFFER = 4096;
//	int FFT_WINDOW = JACK_BUFFER;
	int HALF_BUFFER = DISK_BUFFER / 2;
	int FMT_CHUNK_ID = 0x20746D66;
	int DATA_CHUNK_ID = 0x61746164;
	int RIFF_CHUNK_ID = 0x46464952;
	int RIFF_TYPE_ID = 0x45564157;
	int VALID_BITS = 16; // 2 bytes unsigned, 0x0002 (2) to 0xFFFF (65,535)

	/**Scaling factor for converting to a normalized double. If more than 8 VALID_BITS,
	 * data is signed. Conversion by multiplying magnitude of max positive value*/
	float FLOAT_SCALE = (Long.MAX_VALUE >> (64 - VALID_BITS));
	/** Number of bytes required to store a single sample */
	int SAMPLE_BYTES = (VALID_BITS + 7) / 8;
	/** 2 bytes unsigned, 0x0001 (1) to 0xFFFF (65,535) */
	int BLOCK_ALIGN = SAMPLE_BYTES * 2;

    int LEFT = 0;
	int RIGHT = 1;
	int STEREO = 2;
	int MONO = 1;
	String WAV_EXT = ".wav";

}
