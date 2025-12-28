package net.judah.seq.chords;

import net.judah.api.Chord;

public interface ChordListener {

	void chordChange(Chord from, Chord to);
	
}
