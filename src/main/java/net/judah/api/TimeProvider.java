package net.judah.api;

public interface TimeProvider extends TimeNotifier {

	float getTempo();
	/**@return true if the operation is supported and successful */
	boolean setTempo(float tempo);

	/** beats per bar */
	int getMeasure();
	void setMeasure(int bpb);

	/**@return in milliseconds, -1 if transport not started */
	long getLastPulse();

	void begin();
	void end();
//
//	/**@return in milliseconds, -1 if request is not valid in context */
//	long getNextPulse();

}
