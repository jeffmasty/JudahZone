package net.judah.gui.settable;

import java.util.ArrayList;

import lombok.Getter;
import net.judah.seq.track.MidiTrack;
import net.judah.seq.track.NoteTrack;
import net.judah.util.RTLogger;

@Getter
public class Program extends SetCombo<String> {

	private static final ArrayList<Program> instances = new ArrayList<>();

	private final NoteTrack track;
	private final int ch;

	public Program(NoteTrack t) {
		super(t.getPatches(), t.getProgram());
		track = t;
		ch = track.getCh();
		instances.add(this);
	}

	@Override
	protected void action() {
		if (getSelectedItem() == null)
			return;
		String change = getSelectedItem().toString();
		track.progChange(change);
	}

	public static void update(Program data) {
		String current = data.track.getProgram();
		if (current == null) { // TODO
			RTLogger.warn(data.track + " Program", "no preset set");
			return;
		}
		for (Program c : instances)
			if (c.track.equals(data.track)) {
				if (false == current.equals(c.getSelectedItem()))
					c.override(current);
			}
	}

	public static Program first(MidiTrack track) {
		for (Program c : instances)
			if (c.track.equals(track))
				return c;
		return null;
	}

//	public static Program first(ZoneMidi synth, int ch) {
//		for (Program c : instances)
//			if (c.midiOut == synth && c.ch == ch)
//				return c;
//		return null;
//	}

}
