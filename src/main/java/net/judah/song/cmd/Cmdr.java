package net.judah.song.cmd;

public interface Cmdr {

	String[] getKeys();

	Object resolve(String key);
	
	void execute (Param p);
	
}
