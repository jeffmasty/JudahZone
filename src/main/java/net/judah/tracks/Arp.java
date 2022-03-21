package net.judah.tracks;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.ShortMessage;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;


public class Arp {

	@Data
	public static class Pattern {
		public final int[] seq;
		public final String name;
	}
	
	public static Pattern[] PATTERNS = {
			new Pattern(new int[] {0, 4, 7}, "Major"),
			new Pattern(new int[] {0, 3, 7}, "Minor"),
			new Pattern(new int[] {0, 4, 7, 11}, "Maj7"),
			new Pattern(new int[] {0, 4, 7, 10}, "Dom7"),
			new Pattern(new int[] {0, 3, 7, 10}, "Min7")
	
	};
	
	@Getter private int step = -1;
	@Setter @Getter private Pattern pattern;

	public Arp(Pattern pattern) {
		this.pattern = pattern;
	}
	
	public ShortMessage apply(ShortMessage midi, JackPort midiOut) throws InvalidMidiDataException {
		step++;
		if (step == pattern.seq.length)
			step = 0;
		return new ShortMessage(midi.getCommand(), midi.getChannel(), midi.getData1() + pattern.seq[step], midi.getData2());
	}

}
