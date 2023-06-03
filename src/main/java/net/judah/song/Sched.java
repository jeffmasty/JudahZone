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
	public float amp = 0.65f;            
	
	public Sched(Sched clone) {
		launch = clone.launch;
		active = clone.active;
		cycle = clone.cycle;
		amp = clone.amp;
	}

	public Sched(boolean isDrums) {
		cycle = isDrums ? CYCLE.AB : cycle;
	}
	
}
