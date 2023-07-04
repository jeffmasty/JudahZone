package net.judah.api;

public interface TimeProvider extends TimeNotifier {

	float getTempo();
	/**@return true if the operation is supported and successful */
	void writeTempo(int tempo);

	void begin();
	void end();
	
	int getMeasure();
	int getBeat();

	//	/**@return in milliseconds, -1 if request is not valid in context */
	//	long getNextPulse();
	//  void setMeasure(int bpb);

}
