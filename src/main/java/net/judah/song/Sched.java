package net.judah.song;

import lombok.Data;
import lombok.NoArgsConstructor;
import net.judah.seq.CYCLE;

/** Serialized midi track state for a given Scene */
@Data @NoArgsConstructor
public class Sched {
	
	public boolean active;               
	public CYCLE cycle = CYCLE.AB;     
	public int launch;                   
	public float amp = 0.6f;
	// track name? currently hardcoded track order
	// arp mode?
	
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
