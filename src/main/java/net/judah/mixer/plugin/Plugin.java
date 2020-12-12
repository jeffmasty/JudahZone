package net.judah.mixer.plugin;

import lombok.Data;
import net.judah.mixer.LineType;

@Data
public class Plugin {

	private final String name;
	private final int index;
	private final LineType type;
	
}
