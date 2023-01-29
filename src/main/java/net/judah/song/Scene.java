package net.judah.song;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.judah.JudahZone;

@Data @AllArgsConstructor @NoArgsConstructor
public class Scene {
	
	UUID id = UUID.randomUUID();
	Trigger type = Trigger.BAR;
	String notes;
	ParamList commands = new ParamList();
	List<Sched> tracks = new ArrayList<>();
	
	public Scene(int size) { // INIT
		for (int i = 0; i < size; i++)
			tracks.add(new Sched());
	}

	public Scene(int size, Trigger t) {
		this(size);
		type = t;
	}

	public Scene(List<Sched> state) {
		tracks.addAll(state);
	}

	@Override
	public Scene clone() {
		Scene result = new Scene();
		result.setCommands(new ParamList(commands));
		result.setTracks(new ArrayList<>(tracks));
		result.notes = notes;
		result.type = type;
		return result;
	}

	@Override
	public String toString() {
		return "" + JudahZone.getCurrent().getScenes().indexOf(this);
	}

	
}
