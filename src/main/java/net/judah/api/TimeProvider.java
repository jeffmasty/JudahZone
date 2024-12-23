package net.judah.api;

public interface TimeProvider {

	void addListener(TimeListener l);

	boolean removeListener(TimeListener l);

	void begin();

	void end();

	void setTimeSig(Signature time);

	Signature getTimeSig();

	int getMeasure();

	int getBeat();

	void reset();

}
