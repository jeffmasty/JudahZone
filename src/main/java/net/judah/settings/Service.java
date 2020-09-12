package net.judah.settings;

import java.util.HashMap;
import java.util.List;

import net.judah.util.Tab;

public interface Service {

	String getServiceName();

	List<Command> getCommands();

	void execute(Command cmd, HashMap<String, Object> props) throws Exception;

	void close();

	Tab getGui();

}
