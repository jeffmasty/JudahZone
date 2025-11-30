package net.judah.song;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;
import lombok.NoArgsConstructor;
import net.judah.JudahZone;
import net.judah.seq.Seq;
import net.judah.song.cmd.ParamList;

@Data @NoArgsConstructor
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
		ArrayList<Sched> trax = new ArrayList<>(tracks.size());
		for (Sched source : tracks)
			trax.add(new Sched(source));
		result.setTracks(trax);
		for (String ch : fx)
			result.fx.add(ch);
		result.notes = notes;
		result.type = type;
		return result;
	}

	@Override
	public String toString() {
		return "" + JudahZone.getOverview().getSong().getScenes().indexOf(this);
	}


}
