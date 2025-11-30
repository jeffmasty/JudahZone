package net.judah.song;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.Data;
import lombok.NoArgsConstructor;
import net.judah.seq.arp.Arp;
import net.judah.seq.arp.ArpInfo;
import net.judah.seq.track.Cycle;

/** Serialized MidiTrack state for a given Scene */
@Data @NoArgsConstructor
public class Sched {

	public boolean active;
	public Cycle cycle = Cycle.AB;
	public int launch;
	public float amp = 0.5f;
	public Arp mode;
	private String program;
	@JsonInclude(Include.NON_NULL)
	private ArpInfo arp = new ArpInfo(); // mode, octaves

	public Sched(Sched clone) {
		launch = clone.launch;
		active = clone.active;
		cycle = clone.cycle;
		amp = clone.amp;
		program = clone.program;
		arp = clone.arp == null ? null : new ArpInfo(clone.arp);
	}

	public Sched(boolean synth) {
		if (synth)
			arp = new ArpInfo();
	}


}
