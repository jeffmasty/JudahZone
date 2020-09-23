package net.judah.song;

import java.util.HashMap;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.judah.settings.Command;

@Data @AllArgsConstructor @NoArgsConstructor
public class Trigger {
	public enum Type {
		ABSOLUTE, RELATIVE, MIDI
	}
	
	public Trigger(long timestamp, Command cmd) {
		this(Type.ABSOLUTE, timestamp, 0l, cmd.getService().getServiceName(), cmd.getName(), "", new HashMap<>());
	}
	
	Type type;
	Long timestamp;
	Long duration; // TODO
	
	String service;
	String command;
	String notes;
	HashMap<String, Object> params;
	
}
