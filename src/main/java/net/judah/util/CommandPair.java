package net.judah.util;

import java.util.HashMap;

import lombok.Data;
import net.judah.api.Command;

@Data
public class CommandPair {

	private final Command command;
	private final HashMap<String, Object> props;
	
	@Override
	public String toString() {
		return command + " " + Constants.prettyPrint(props);
	}
	
}
