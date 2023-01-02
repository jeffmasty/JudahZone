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
	
	Trigger type = Trigger.BAR;
	String notes;
	ParamList params = new ParamList();
	List<Sched> tracks = new ArrayList<>();
	UUID id = UUID.randomUUID();
	
	public Scene(Trigger type, int size) { // INIT
		this.type = type;
		for (int i = 0; i < size; i++)
			tracks.add(new Sched());
	}

	public Scene(List<Sched> state) {
		tracks.addAll(state);
	}
	
	@Override
	public Scene clone() {
		Scene result = new Scene();
		result.setNotes(notes);
		result.setType(type);
		result.setParams(new ParamList(params));
		result.setTracks(new ArrayList<>(tracks));
		return result;
	}

	@Override
	public String toString() {
		return "" + JudahZone.getCurrent().getScenes().indexOf(this);
	}

	
}
