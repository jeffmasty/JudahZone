package net.judah.api;

/**Participates in real time audio processing, can respond to some commands*/
public interface PlayAudio {

	public enum Type {
		/** play once */ ONE_SHOT,
		/** play on repeat */ LOOP
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
