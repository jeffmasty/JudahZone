package net.judah.seq.track;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import net.judah.drums.fm.FMKit;
import net.judah.drums.noise.NoiseKit;
import net.judah.drums.pluck.PluckKit;
import net.judah.drums.wav.WavKit;
import net.judah.mixer.Channels.RegisteredDrums;
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
	private String channel;
	// DrumSample kit name/net.jz.DrumSynth
	@JsonInclude(Include.NON_NULL)
	private RegisteredDrums kit;

//	@JsonInclude(Include.NON_NULL)
//	private String kit; // for OldSchool: kit db name, for DrumSynth: class name

	public TrackInfo(MidiTrack t) {
		track = t.getName();
		file = t.getFile() == null ? null : t.getFile().getName();
		cue = t.getCue();
		if (t instanceof NoteTrack notes)
			gate = notes.getGate();
		program = t.getProgram();
		if (t instanceof DrumTrack drum) {
			kit = drum.getKit() instanceof NoiseKit ? RegisteredDrums.Noise :
				  drum.getKit() instanceof WavKit ? RegisteredDrums.Wav :
				  drum.getKit() instanceof FMKit ? RegisteredDrums.FM :
				  drum.getKit() instanceof PluckKit ? RegisteredDrums.Pluck : null;
		}
		else if (t instanceof PianoTrack piano)
			arp = piano.getInfo() == null ? null : piano.getInfo();
		else if (t instanceof ChannelTrack ch)
			channel = ch.getChannel().getName();
	}

}
