package net.judah.api;

import java.util.HashMap;
import java.util.List;

public interface Service {

	String getServiceName();

	List<Command> getCommands();

	// void execute(Command cmd, HashMap<String, Object> props) throws Exception;

	void close();
	
	void properties(HashMap<String, Object> props);
	
	
}
