package net.judah.util;

import java.util.HashMap;

import lombok.Data;
import net.judah.api.Command;

@Data
public class CommandWrapper {

	private final Command command;
	private final HashMap<String, Object> props;
	private final int internalCount;
	
	@Override
	public String toString() {
		return command + " " + Constants.prettyPrint(props);
	}
	
}
