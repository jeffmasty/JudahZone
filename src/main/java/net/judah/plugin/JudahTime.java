package net.judah.plugin;

import java.util.HashMap;
import java.util.List;

import lombok.Getter;
import net.judah.settings.Command;
import net.judah.settings.Service;

public class JudahTime implements Service {

	@Getter private final String serviceName = JudahTime.class.getSimpleName();
	
	Command clickTrack;
	
	@Override
	public List<Command> getCommands() {
		return null;
	}

	@Override
	public void execute(Command cmd, HashMap<String, Object> props) throws Exception {
		
	}

	@Override
	public void close() {
		
	}

	
}
