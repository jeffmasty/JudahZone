package net.judah.api;

import java.io.Closeable;
import java.util.Vector;

import javax.sound.midi.Receiver;

import net.judah.seq.track.MidiTrack;

/** javax Receiver with channels and presets */
public interface ZoneMidi extends Receiver, Closeable {

	/** Name of this Receiver */
	String getName();

	/** Midi Channels managed by this receiver */
	Vector<? extends MidiTrack> getTracks();

	/** list of program names */
	String[] getPatches();

	/** program change on default channel
	 * @return true on success */
	boolean progChange(String preset);

	/** @return true if a match is found in patches */
	boolean progChange(String preset, int channel);

	/** current Program name loaded into a managed channel or -1 */
	String getProg(int ch);

	/**@param data2 progChange usually embedded in Midi File
	 * @return name of prog change on success or null */
	String progChange(int data2, int ch);

}
