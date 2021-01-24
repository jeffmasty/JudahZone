package net.judah.song;

import java.util.HashMap;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.judah.JudahZone;
import net.judah.api.Command;
import net.judah.song.Edits.Copyable;

@Data @AllArgsConstructor @NoArgsConstructor
public class Trigger implements Copyable {
	public static enum Type {
		INIT, ABS, REL, TRIG, END
	}

	public Trigger(long timestamp, Command cmd) {
		this(Type.ABS, timestamp, cmd.getName(), "", new HashMap<>(), cmd);
	}

	Type type = Type.INIT;
	Long timestamp = 0l;

	String command;
	String notes;
	HashMap<String, Object> params;

	@JsonIgnore
	private transient Command cmd;

	public Command getCmd() {
		if (cmd == null)
			cmd = JudahZone.getCommands().find(command);
		return cmd;
	}

	public boolean go(int count) {
		if (type == Type.INIT && count < 0) return true;
		if (type == Type.ABS && timestamp == count) return true;
		if (type == Type.REL && timestamp == 0) return true;
		return false;
	}

	@Override
	public Trigger clone() {
		Trigger result = new Trigger();
		result.setCommand(command);
		result.setTimestamp(timestamp);
		result.setNotes(notes);
		result.setParams(new HashMap<>(params));
		result.setType(type);
		return result;
	}

}

