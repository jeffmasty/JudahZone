package net.judah.api;

public interface RecordAudio extends PlayAudio {

	/** start or stop recording */
	void capture(boolean onOrOff);

	boolean isRecording();

}
