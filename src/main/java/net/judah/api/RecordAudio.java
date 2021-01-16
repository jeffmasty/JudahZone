package net.judah.api;

public interface RecordAudio extends ProcessAudio {

	/** start or stop recording */
	void record(boolean active);
	
	AudioMode isRecording();
	
}
