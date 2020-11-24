package net.judah.settings;

import java.util.HashMap;

import lombok.Data;

@Data
public class CommandPair {

	private final Command command;
	private final HashMap<String, Object> props;
	
}
