package net.judah.api;

import java.io.Closeable;
import java.util.Vector;

import javax.sound.midi.Receiver;

import net.judah.seq.track.MidiTrack;

public interface MidiReceiver extends Receiver, Closeable {

	String getName();
	
	/** Midi Channels managed by this receiver */
	Vector<MidiTrack> getTracks();
	
	String[] getPatches();
	
	boolean progChange(String preset);
	
	/** true if a match is found in patches, no exception thrown */
	boolean progChange(String preset, int channel);
	
	String getProg(int ch);
	
}
