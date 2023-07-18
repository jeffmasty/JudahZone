package net.judah.seq.track;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.judah.seq.arp.ArpInfo;

@Data @AllArgsConstructor @NoArgsConstructor
public class TrackInfo {

	private String track;
	private String file;
	private Cue cue = Cue.Bar;
	private Gate gate = Gate.SIXTEENTH;
	
	// LEGACY
	@JsonInclude(Include.NON_NULL)
	private String program;
	@JsonInclude(Include.NON_NULL)
	private ArpInfo arp; // mode, octaves
	
	public TrackInfo(String track, boolean isDrums) {
		this.track = track;
	}
	 
	public TrackInfo(MidiTrack t) {
		track = t.getName();
		file = t.getFile() == null ? null : t.getFile().getName();
		cue = t.getCue();
		gate = t.getGate();
	}
	
}
