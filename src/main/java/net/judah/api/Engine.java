package net.judah.api;

import java.nio.FloatBuffer;

/** internal Sound generators that respond to Midi (synths, drum machines) */
public interface Engine extends MidiReceiver {

	FloatBuffer[] getBuffer();
	
	boolean hasWork();
	
	
}
