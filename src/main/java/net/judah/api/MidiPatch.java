package net.judah.api;

public class MidiPatch {
	public final int group;
	public final int index;
	public final String name;

	public MidiPatch (int group, int index, String name) {
		this.group = group;
		this.index = index;
		this.name = name;
	}

	@Override
	public String toString() {
		return name;
	}

}
