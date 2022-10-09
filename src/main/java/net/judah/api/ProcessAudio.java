package net.judah.api;

import net.judah.looper.Recording;

/**Participates in real time audio processing, can respond to some commands*/
public interface ProcessAudio {

	public enum Type {ONE_SHOT, FREE, SOLO, DRUMTRACK, SEQUENCED}

	AudioMode isPlaying();

	Recording getRecording();
	
	void setTapeCounter(int i);
	
	void readRecordedBuffer();

	void clear();
}
