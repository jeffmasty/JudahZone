package net.judah.synth;

import java.io.Closeable;
import java.util.Vector;

import javax.sound.midi.Receiver;

import net.judah.seq.track.NoteTrack;

/** javax Receiver with channels and presets */
public interface ZoneMidi extends Receiver, Closeable {

	/** Name of this Receiver */
	String getName();

	/** Midi Channels managed by this receiver */
	Vector<? extends NoteTrack> getTracks();

	NoteTrack getTrack();

	/** list of program names */
	String[] getPatches();

	/**@param data2 progChange usually embedded in Midi File
	 * @return name of prog change on success or null */
	String progChange(int data2, int ch);

	void process(); // tied to FxChain/Channel.process()
	void mix(float[] left, float[] right); // tied to FxChain/Channel.process()

}
