package net.judah.api;

import java.io.Closeable;
import java.util.List;

import javax.sound.midi.Receiver;

import net.judah.fx.Gain;
import net.judah.midi.MidiPort;

public interface MidiReceiver extends Receiver, Closeable {

	List<Integer> getActives(); // ?
	
	String getName();
	
	MidiPort getMidiPort();
	
	String[] getPatches();
	
	boolean progChange(String preset);
	
	/** true if a match is found in patches, no exception thrown */
	boolean progChange(String preset, int channel);
	
	String getProg(int ch);
	
	boolean isMuteRecord();
	
	boolean isMono();
	
	Gain getGain();
	
}
