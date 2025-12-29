package net.judah.seq.chords;

import judahzone.api.Chord;

public interface ChordListener {

	void chordChange(Chord from, Chord to);
	
}
