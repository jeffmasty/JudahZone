package net.judah.api;

public interface TimeNotifier {

	void addListener(TimeListener l);
	void removeListener(TimeListener l);
	
}
