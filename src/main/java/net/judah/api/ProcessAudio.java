package net.judah.api;

import net.judah.looper.Recording;

/**Participates in real time audio processing, can respond to some commands*/
public interface ProcessAudio {

	public enum Type {ONE_SHOT, FREE, SOLO, DRUMTRACK, SEQUENCED}

	/** in Real-Time thread */
	void process();

	AudioMode isPlaying();

	Recording getRecording();

//	/** destination for audio output */
//	void setOutputPorts(List<JackPort> output);

	void clear();
}
