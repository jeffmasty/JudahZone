package net.judah.settings;

import java.util.List;
import java.util.Properties;

import net.judah.util.Tab;

public interface Service {

	String getServiceName();

	List<Command> getCommands();

	void execute(Command cmd, Properties props) throws Exception;

	void close();

	Tab getGui();

}
