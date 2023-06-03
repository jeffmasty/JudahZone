package net.judah.seq.chords;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.judah.seq.Poly;

@AllArgsConstructor @Getter
public class ChordEvent {

	private final Poly chord;
	@Setter private long tick;
	
}
