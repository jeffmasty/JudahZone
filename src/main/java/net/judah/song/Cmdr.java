package net.judah.song;

public interface Cmdr {

	String[] getKeys();

//	int value(String key);

	String lookup(int value); // legacy
	
	Object resolve(String key);
	
	void execute (Param p);
	
}
