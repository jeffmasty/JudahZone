package net.judah.song;

import lombok.Data;
import lombok.NoArgsConstructor;
import net.judah.seq.arp.Mode;
import net.judah.seq.track.Cycle;

/** Serialized midi track state for a given Scene */
@Data @NoArgsConstructor
public class Sched {
	
	public boolean active;               
	public Cycle cycle = Cycle.AB;     
	public int launch;                   
	public float amp = 0.6f;
	public Mode mode;
	// track name? currently hardcoded track order
	// arp mode?
	
	public Sched(Sched clone) {
		launch = clone.launch;
		active = clone.active;
		cycle = clone.cycle;
		amp = clone.amp;
	}
	
}
