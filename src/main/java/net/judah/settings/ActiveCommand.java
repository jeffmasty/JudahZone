package net.judah.settings;

import java.util.HashMap;

// processes data2, if non-zero, sets #ACTIVE_PARAM to true;
public class ActiveCommand extends DynamicCommand {

	public ActiveCommand(String name, Service service, HashMap<String, Class<?>> props, String description) {
		super(name, service, props, description);
		props.put(Command.ACTIVE_PARAM, Boolean.class);
	}
	
	@Override public void processMidi(int data2, HashMap<String, Object> props) {
		props.put(ACTIVE_PARAM, data2 > 0); 
	}

}
