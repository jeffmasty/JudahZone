package net.judah.gui.settable;

import judahzone.api.MidiOut;
import lombok.Getter;
import net.judah.seq.track.NoteTrack;



/** try to keep in sync with owner of actual progchange truth, which is a MidiOut instance (which a MidiTrack has).
 * SetCombo means fly through the options without firing changes (midi controller wheel paired with activate pad)
 * */
public class Program extends SetCombo<String> {

	@Getter private final MidiOut midiOut;

	public Program(MidiOut out) {
		super(out.getPatches(), out.getProgram());
		this.midiOut = out;
	}

	public Program(NoteTrack t) {
		super(t.getPatches(), t.getProgram());
		midiOut = t.getMidiOut();
	}

	@Override protected void action() {
		if (getSelectedItem() == null)
			return;
		String change = getSelectedItem().toString();
		midiOut.progChange(change);
	}

	public void update() {
		if (getSelectedItem() == null && midiOut.getProgram() == null)
			return;
		if (getSelectedItem() != null && getSelectedItem().equals(midiOut.getProgram()))
			return;
		override(midiOut.getProgram()); // DataRace

	}

}
