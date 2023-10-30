package net.judah.api;

public interface RecordAudio extends PlayAudio {

	void record(boolean onOrOff);
	
	boolean isRecording();
	
}
