package net.judah.api;

import net.judah.omni.Recording;

/**Participates in real time audio processing, can respond to some commands*/
public interface PlayAudio {

	public enum Type {
		/** a sample */ ONE_SHOT,
		/** strict sync to clock */ SYNC,
		/** sync to clock but unknown measures at record start*/ BSYNC,
		/** not sync'ed to clock */ FREE,
		/** record a specific audio channel */ SOLO
		}

	void setRecording(Recording r);

	Recording getRecording();

	void play(boolean onOrOff);

	boolean isPlaying();

	/** jack frame count */
	int getLength();

	float seconds();

	void rewind();
}
