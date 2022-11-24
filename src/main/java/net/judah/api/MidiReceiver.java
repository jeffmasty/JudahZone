package net.judah.api;

import java.io.Closeable;

import javax.sound.midi.Receiver;

import net.judah.midi.MidiPort;

public interface MidiReceiver extends Receiver, Closeable {

	String getName();
	
	String[] getPatches();
	
	MidiPort getMidiPort();
	
	void progChange(String preset);
	
	void progChange(String preset, int channel);
	
	int getProg(int ch);
}
