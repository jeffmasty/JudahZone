package net.judah.song;

import java.util.Arrays;
import java.util.HashMap;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.judah.api.Command;
import net.judah.sequencer.Sequencer;
import net.judah.song.Edits.Copyable;

@AllArgsConstructor @NoArgsConstructor @Data
public class Link implements Copyable {

	private String name;
	private String command;
	private byte[] midi;
	private HashMap<String, Object> props;

	@JsonIgnore
	private transient Command cmd;
	
	public Command getCmd() {
		if (cmd == null) 
			cmd = Sequencer.getCurrent().getCommander().find(command);
		return cmd;
		
	}
	
	@Override
	public Link clone() throws CloneNotSupportedException {
		return new Link(name, command, Arrays.copyOf(midi, midi.length), new HashMap<>(props), cmd);
	}
}

