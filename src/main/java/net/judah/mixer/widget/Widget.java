package net.judah.mixer.widget;

import java.util.HashMap;

import lombok.Data;
import net.judah.mixer.Channel;

@Data
public abstract class Widget {
	//	final protected String[] inPorts;
//	final protected String[] outPorts;
	protected Channel.Type type;
	protected boolean active;
	
	// current params
	protected HashMap<String, Object> params;
	
	protected HashMap<String, Object> options;
	
	
}
