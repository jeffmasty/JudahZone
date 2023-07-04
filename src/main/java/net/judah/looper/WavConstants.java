package net.judah.looper;
// Wav file IO class
// A.Greensted
// http://www.labbookpages.co.uk

import net.judah.util.Constants;

public interface WavConstants {
	int BUFFER_SIZE = 4096;
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
	int JACK_SIZE = Constants.bufSize();
	int S_RATE = Constants.sampleRate();
	int LEFT = Constants.LEFT;
	int RIGHT = Constants.RIGHT;
	int STEREO = Constants.STEREO;
	
	float seconds();
	
}
