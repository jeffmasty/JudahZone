package net.judah.api;

import java.nio.FloatBuffer;

import javax.sound.midi.Receiver;

/** internal Sound generators that respond to Midi (synths, drum machines) */
public interface Engine extends Receiver {

	FloatBuffer[] getBuffer();
	
	boolean hasWork();
	
	boolean isMuteRecord();
	
	String getName();
	
	void progChange(String preset);
	
}
