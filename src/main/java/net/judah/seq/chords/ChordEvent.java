package net.judah.seq.chords;

import java.util.List;

// Not Used
public class ChordEvent {

	private final List<Integer> chord;
	private long tick;

	public ChordEvent(List<Integer> chord, long tick) {
		this.chord = chord;
		this.tick = tick;
	}

	public List<Integer> getChord() { return chord; }
	public long getTick() { return tick; }
	public void setTick(long t) { this.tick = t; }

}
