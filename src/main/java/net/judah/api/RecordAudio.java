package net.judah.api;

public interface RecordAudio {

	/** start or stop recording */
	void capture(boolean onOrOff);

	boolean isRecording();

	void clear();
}
