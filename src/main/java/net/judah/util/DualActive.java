package net.judah.util;

import java.util.HashMap;

import net.judah.api.Command;

/** TODO simultaneously activate/deactivate 2 commands (ie., and octaver and an EQ) */
public class DualActive extends Command {

	DualActive() {
		super("", ""); // props: command1, index1, command2, index2
	}
	
	@Override
	public void execute(HashMap<String, Object> props, int midiData2) throws Exception {
		// set active and execute both based on midi input

	}

}
