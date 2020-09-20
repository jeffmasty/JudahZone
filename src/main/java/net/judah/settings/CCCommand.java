package net.judah.settings;

import java.util.HashMap;

public class CCCommand extends DynamicCommand {

	public CCCommand(String name, Service service, HashMap<String, Class<?>> props, String description) {
		super(name, service, props, description);
	}

	@Override
	public void processMidi(int data2, HashMap<String, Object> props) {
		// TODO Auto-generated method stub

	}

}
