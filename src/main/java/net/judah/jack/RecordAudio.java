package net.judah.jack;

import java.util.List;

import net.judah.mixer.MixerPort;

public interface RecordAudio extends ProcessAudio {

	/** start or stop recording */
	void record(boolean active);
	
	AudioMode isRecording();
	
	/** destination for audio output */
	void setInputPorts(List<MixerPort> input);

}
