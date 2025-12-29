package net.judah.api;

import java.util.List;

import javax.sound.midi.ShortMessage;

/** base for arpeggiator */
public abstract class Algo {

	protected int range;

	public final void setRange(int range) { this.range = range; };
	public final int getRange() { return range; }

	/**
	 * @param bass reference bass note/octave
	 * @param chord input chord to transform
	 * @param result transformation
	 */
	public abstract void process(ShortMessage bass, Chord chord, List<Integer> result);

	/** if needed, subclasses can reset on chord changes */
	public void change() {

	}

}
