package net.judah.song;

import java.util.HashMap;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.judah.JudahZone;
import net.judah.settings.Command;
import net.judah.song.Edits.Copyable;

@Data @AllArgsConstructor @NoArgsConstructor
public class Trigger implements Copyable {
	public enum Type {
		ABSOLUTE, RELATIVE, MIDI
	}
	
	public Trigger(long timestamp, Command cmd) {
		this(Type.ABSOLUTE, timestamp, 0l, cmd.getName(), "", new HashMap<>(), null);
	}
	
	Type type = Type.ABSOLUTE;
	Long timestamp;
	Long duration; 
	
	String command;
	String notes;
	HashMap<String, Object> params;
	
	@JsonIgnore
	private transient Command cmd;
	
	public Command getCmd() {
		if (cmd == null) 
			cmd = JudahZone.getCurrentSong().getCommander().find(command);
		return cmd;
		
	}

	@Override
	public Trigger clone() {
		Trigger result = new Trigger();
		result.setCommand(command);
		result.setDuration(duration);
		result.setTimestamp(timestamp);
		result.setNotes(notes);
		result.setParams(new HashMap<>(params));
		result.setType(type);
		return result;
	}
	
}

