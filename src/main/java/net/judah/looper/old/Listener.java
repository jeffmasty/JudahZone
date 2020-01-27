package net.judah.looper.old;

public interface Listener {

	/** WARNING: in the real time thread. */
	void newFrame(int frame, long time);

	void sealTheDeal(int lastFrame, long time);

}
