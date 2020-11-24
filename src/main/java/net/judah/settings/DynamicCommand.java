package net.judah.settings;

import java.util.HashMap;


public abstract class DynamicCommand extends Command {
	
	public DynamicCommand(String name, Service service, HashMap<String, Class<?>> props, String description) {
		super(name, service, props, description);
	}

	public DynamicCommand(String name, Service service, String description) {
		super(name, service, description);
	}

	/** process dynamic data */
	public abstract void processMidi(int data2, HashMap<String, Object> props);
}
