package net.judah.song;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.judah.JudahZone;
import net.judah.seq.Seq;

@Data @AllArgsConstructor @NoArgsConstructor
public class Scene {
	
	@JsonIgnore UUID id = UUID.randomUUID();
	Trigger type = Trigger.BAR;
	String notes = "";
	ParamList commands = new ParamList();
	List<Sched> tracks = new ArrayList<>();
	List<String> fx = new ArrayList<>();
	 
	public Scene(Seq seq) {
		seq.init(tracks);
	}
	
	public Scene(List<Sched> state) {
		state.forEach(clone -> tracks.add(new Sched(clone)));
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
