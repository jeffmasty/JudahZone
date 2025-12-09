package net.judah.gui.settable;

import lombok.Getter;
import net.judah.seq.track.MidiTrack;

public class Program extends SetCombo<String> {

	@Getter private final MidiTrack track;

	public Program(MidiTrack t) {
		super(t.getPatches(), t.getProgram());
		track = t;
	}

	@Override protected void action() {
		if (getSelectedItem() == null)
			return;
		String change = getSelectedItem().toString();
		track.progChange(change);
	}

	public void update() {
		if (getSelectedItem() == null && track.getProgram() == null)
			return;
		if (getSelectedItem() != null && getSelectedItem().equals(track.getProgram()))
			return;
		setSelectedItem(track.getProgram());
	}

}
