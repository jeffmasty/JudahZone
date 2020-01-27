package net.judah.settings;

import java.util.HashMap;

@SuppressWarnings("serial")
public class ExternalCommand extends Command {

	public ExternalCommand(String name, Service service, HashMap<String, Class<?>> props, String description) {
		super(name, service, props, description);

	}

}
