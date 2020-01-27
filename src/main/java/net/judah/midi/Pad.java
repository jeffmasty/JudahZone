package net.judah.midi;

public class Pad {

	public static final int INDEX_TO_MIDI_OFFSET = 36;

	final int index;
	PadAction[] actions;

	public static class PadAction {

	}


	public Pad(int index, PadAction action) {
		this.index = index;
		actions = new PadAction[1];
		actions[0] = action;
	}
}
