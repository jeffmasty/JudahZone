package net.judah.settings;

import java.util.HashMap;

public class MidiCommand extends Command {

	public MidiCommand(String name, Service service, HashMap<String, Class<?>> props, String description) {
		super(name, service, props, description);
		// TODO Auto-generated constructor stub
	}

	public MidiCommand(String name, Service service, String description) {
		super(name, service, description);
		// TODO Auto-generated constructor stub
	}

}
