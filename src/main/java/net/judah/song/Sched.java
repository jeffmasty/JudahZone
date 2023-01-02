package net.judah.song;

import lombok.Data;
import lombok.NoArgsConstructor;
import net.judah.seq.Cycle;

@Data @NoArgsConstructor
public class Sched {
	
	// serializeable state
	public boolean active;
	public Cycle cycle = Cycle.AB;
	public int launch; 
	public String preset;
	public float amp = 0.8f;
	
	public Sched(Sched clone) {
		launch = clone.launch;
		active = clone.active;
		cycle = clone.cycle;
		preset = clone.preset;
		amp = clone.amp;
	}
	
}
