package net.judah.seq.track;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import net.judah.seq.arp.ArpInfo;

@Data @AllArgsConstructor @NoArgsConstructor @ToString
public class TrackInfo {

	private String track;
	private String file;
	private Cue cue = Cue.Bar;
	private Gate gate = Gate.SIXTEENTH;
	@JsonInclude(Include.NON_NULL)
	private String program;

	@JsonInclude(Include.NON_NULL)
	private ArpInfo arp; // mode, octaves
	@JsonInclude(Include.NON_NULL)
	private String channel; // Line/Engine

	public TrackInfo(MidiTrack t) {
		track = t.getName();
		file = t.getFile() == null ? null : t.getFile().getName();
		cue = t.getCue();
		if (t instanceof NoteTrack notes)
			gate = notes.getGate();
		program = t.getProgram();
		if (t instanceof PianoTrack piano)
			arp = piano.getInfo() == null ? null : piano.getInfo();
		else if (t instanceof ChannelTrack ch)
			channel = ch.getChannel().getName();
	}

}
