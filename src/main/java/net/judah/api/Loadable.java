package net.judah.api;

import java.io.IOException;
import java.util.HashMap;

public interface Loadable {

	void load(HashMap<String, Object> props) throws IOException;
	
	void close();
}
