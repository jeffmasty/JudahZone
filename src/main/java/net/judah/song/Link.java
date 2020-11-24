package net.judah.song;

import java.util.Arrays;
import java.util.HashMap;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.judah.JudahZone;
import net.judah.settings.Command;
import net.judah.song.Edits.Copyable;

@AllArgsConstructor @NoArgsConstructor @Data
public class Link implements Copyable {

	private String name;
	private String service;
	private String command;
	private byte[] midi;
	private HashMap<String, Object> props;

	@JsonIgnore
	private transient Command cmd;
	
	public Command getCmd() {
		if (cmd == null) 
			cmd = JudahZone.getCurrentSong().getCommander().find(service, command);
		return cmd;
		
	}
	
	@Override
	public Link clone() throws CloneNotSupportedException {
		return new Link(name, service, command, Arrays.copyOf(midi, midi.length), new HashMap<>(props), cmd);
	}
}

