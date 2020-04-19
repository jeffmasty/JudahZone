package net.judah.mixer;

import java.util.HashMap;

import lombok.Data;

@Data
public abstract class Widget {
	public enum Type {SYS, SYNTH, CARLA, MODHOST, INTERNAL}
	
	final protected String[] inPorts;
	final protected String[] outPorts;
	final protected Type type;
	protected boolean active;
	
	// current params
	protected HashMap<String, Object> params;
	
	protected HashMap<String, Object> options;
	
	
}
