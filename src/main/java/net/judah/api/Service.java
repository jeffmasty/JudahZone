package net.judah.api;

import java.util.HashMap;


/** command registry and handle shutdown */
public interface Service {

	void close();

	/** inspect a song's properties onLoad for custom config of this service */
	void properties(HashMap<String, Object> props);
	
	
}
