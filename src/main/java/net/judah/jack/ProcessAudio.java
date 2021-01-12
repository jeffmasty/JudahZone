package net.judah.jack;

import java.util.List;

import org.jaudiolibs.jnajack.JackPort;

import net.judah.looper.Recording;

/**Participates in real time audio processing, can respond to some commands*/
public interface ProcessAudio {

	public enum Type {ONE_TIME, CONTROLLED, DUPLICATE, FREE, SOLO, MASTER, DRUMTRACK}
	
	/** in Real-Time thread */
	void process();
	
	/** stop or play audio in process() thread, if any */
	void play(boolean active);
	
	AudioMode isPlaying();
	
	Recording getRecording();

	/** not below 0 but be prepared for volumes towards 1.5f */
	void setGain(float volume);
	
	/** destination for audio output */
	void setOutputPorts(List<JackPort> output);
	
	void clear();
}
