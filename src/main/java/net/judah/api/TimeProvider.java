package net.judah.api;

public interface TimeProvider extends TimeNotifier {

	float getTempo();
	/**@return true if the operation is supported and successful */
	void writeTempo(int tempo);

	void begin();
	void end();
	
	int getMeasure();
	int getBeat();
	Signature getTimeSig();

}
