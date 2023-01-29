package net.judah.song;

import lombok.Data;
import lombok.NoArgsConstructor;
import net.judah.seq.CYCLE;

@Data @NoArgsConstructor
public class Sched {
	
	// serialize state
	public boolean active;
	public CYCLE cycle = CYCLE.AB;
	public int launch; 
	public int amp = 85;
	
	public Sched(Sched clone) {
		launch = clone.launch;
		active = clone.active;
		cycle = clone.cycle;
		amp = clone.amp;
	}
	
}
