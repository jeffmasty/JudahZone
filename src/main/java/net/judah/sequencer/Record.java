package net.judah.sequencer;

import static net.judah.util.Constants.Param.*;

import java.util.HashMap;

import net.judah.api.ActiveType;
import net.judah.api.Command;
import net.judah.util.Console;

public class Record extends Command {

	public Record() {
		super("", "", createTemplate());
	}
	
	@SuppressWarnings("incomplete-switch")
	@Override
	public void execute(HashMap<String, Object> props, int midiData2) throws Exception {
		ActiveType type = ActiveType.valueOf(props.get(TYPE).toString());
		String name = null;
		if (props.containsKey(NAME))
			name = "" + props.get(NAME);
		Integer index = null;
		if (props.containsKey(INDEX))
			try { index = Integer.parseInt(props.get(INDEX).toString()); }
			catch (Throwable t) { Console.debug(t.getMessage() + " for " + props.get(INDEX)); }
		boolean active = parseActive(props);
		
		switch(type) {
		case LOOP:			// by index
			break;
		case MIDITRACK:		// by index
			break;
		case STEPSEQUENCE:  // by name
			break;
		//	case MIDIFILE:
		//	case AUDIOFILE:
		//	case PLUGIN			
		}
		
	}

	public static final HashMap<String, Class<?>> createTemplate() {
		HashMap<String, Class<?>> result = activeTemplate();
		result.put(INDEX, Integer.class);
		result.put(NAME, String.class);
		return result;
	}
	
}
