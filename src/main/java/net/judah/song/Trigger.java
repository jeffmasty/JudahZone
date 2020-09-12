package net.judah.song;

import java.util.HashMap;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @AllArgsConstructor @NoArgsConstructor
public class Trigger {
	public enum Type {
		ABSOLUTE, RELATIVE, MIDI
	}
	
	Type type;
	Long timestamp;
	Long duration; // TODO
	
	String service;
	String command;
	String notes;
	HashMap<String, Object> params;

}
