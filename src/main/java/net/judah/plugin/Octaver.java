package net.judah.plugin;

import static net.judah.settings.Commands.OtherLbls.*;
import static net.judah.util.Constants.Param.*;

import java.io.IOException;
import java.util.HashMap;

import net.judah.api.Command;
import net.judah.api.Loadable;

public class Octaver extends Command implements Loadable {

	private boolean active;
	
	public Octaver() {
		super(OCTAVER.name, OCTAVER.desc);
	}
	
	@Override
	public void execute(HashMap<String, Object> props, int midiData2) throws Exception {
		if (props.containsKey(ACTIVE))
			setActive(parseActive(props));
		else // toggle
			setActive(!active);
	}

	
	
	public void setActive(boolean active) {
		
	}

	@Override
	public void load(HashMap<String, Object> props) throws IOException {
		setActive(false);
		
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub
		
	}
	
}
