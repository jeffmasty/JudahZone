package net.judah.api;

public interface TimeNotifier {

	void addListener(TimeListener l);
	boolean removeListener(TimeListener l);
	
}
