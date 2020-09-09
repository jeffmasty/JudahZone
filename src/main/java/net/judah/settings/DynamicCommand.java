package net.judah.settings;

import java.util.HashMap;


public class DynamicCommand extends Command {
	
	public DynamicCommand(String name, Service service, HashMap<String, Class<?>> props, String description) {
		super(name, service, props, description);
	}

	public DynamicCommand(String name, Service service, String description) {
		super(name, service, description);
	}

	@Override
	public boolean isDynamic() {
		return true;
	}
}
