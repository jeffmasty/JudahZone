package net.judah.api;

import java.io.Closeable;
import java.util.List;

import javax.sound.midi.Receiver;

import net.judah.midi.MidiPort;

public interface MidiReceiver extends Receiver, Closeable {

	List<Integer> getActives();
	
	String getName();
	
	String[] getPatches();
	
	MidiPort getMidiPort();
	
	void progChange(String preset);
	
	void progChange(String preset, int channel);
	
	int getProg(int ch);
	
	void setAmplification(float gain);
	
	float getAmplification();
	
}
