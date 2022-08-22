package net.judah.api;

import java.util.HashMap;
import java.util.List;


/** command registry and handle shutdown */
public interface Service {

	List<Command> getCommands();

	void close();

	/** inspect a song's properties onLoad for custom config of this service */
	void properties(HashMap<String, Object> props);
	
	
}
