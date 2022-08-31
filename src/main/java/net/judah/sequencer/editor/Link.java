package net.judah.sequencer.editor;

import java.util.HashMap;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.judah.JudahZone;
import net.judah.api.Command;
import net.judah.api.Midi;
import net.judah.sequencer.editor.Edits.Copyable;

@AllArgsConstructor @NoArgsConstructor @Data
public class Link implements Copyable {

	private String name;
	private String command;
	private Midi midi;
	private HashMap<String, Object> props;

	@JsonIgnore
	private transient Command cmd;

	public Command getCmd() {
		if (cmd == null)
			cmd = JudahZone.getCommands().find(command);
		return cmd;

	}

	@Override
	public Link clone() throws CloneNotSupportedException {
		return new Link(name, command, Midi.copy(midi), new HashMap<>(props), cmd);
	}
}

