package net.judah.api;

import net.judah.looper.Recording;

/**Participates in real time audio processing, can respond to some commands*/
public interface PlayAudio {

	public enum Type {ONE_SHOT, SYNC, BSYNC, FREE, SOLO}

	void setRecording(Recording r);
	
	Recording getRecording();
	
	void play(boolean onOrOff);
	
	boolean isPlaying();

	/** jack frame count */
	int getLength();
	
	float seconds();
	
	void clear();
	
	void rewind();
}
