package net.judah.song;

import java.io.File;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.judah.seq.Cue;
import net.judah.seq.Gate;
import net.judah.seq.MidiTrack;
import net.judah.seq.arp.ArpInfo;

@Data @AllArgsConstructor @NoArgsConstructor
public class TrackInfo {

	private String track;
	private File file;
	private String program;
	private Cue cue = Cue.Bar;
	private Gate gate = Gate.SIXTEENTH;
	
	@JsonInclude(Include.NON_NULL)
	private ArpInfo arp; // mode, octaves
	
	public TrackInfo(String track, boolean isDrums) {
		this.track = track;
		arp = isDrums ? null : new ArpInfo();
	}
	 
	public TrackInfo(MidiTrack t) {
		track = t.getName();
		file = t.getFile();
		program = t.getMidiOut().getProg(t.getCh());
		cue = t.getCue();
		gate = t.getGate();
		arp = t.isDrums() ? null : new ArpInfo(t.getArp());
	}
	
}
