package net.judah.settings;

import java.util.HashMap;

public abstract class ClosableCommand extends Command {

	public ClosableCommand(String name, Service service, HashMap<String, Class<?>> props, String description) {
		super(name, service, props, description);
	}

	public ClosableCommand(String name, Service service, String description) {
		super(name, service, description); 
	}
	

	public abstract void close();
	
}
