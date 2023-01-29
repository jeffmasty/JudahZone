package net.judah.api;

public interface TimeProvider extends TimeNotifier {

	float getTempo();
	/**@return true if the operation is supported and successful */
	void writeTempo(int tempo);

	/** beats per bar */
	int getMeasure();

	void begin();
	void end();

	//	/**@return in milliseconds, -1 if request is not valid in context */
	//	long getNextPulse();
	//  void setMeasure(int bpb);

}
