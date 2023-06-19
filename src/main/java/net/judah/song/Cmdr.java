package net.judah.song;

public interface Cmdr {

	String[] getKeys();

	Object resolve(String key);
	
	void execute (Param p);
	
}
