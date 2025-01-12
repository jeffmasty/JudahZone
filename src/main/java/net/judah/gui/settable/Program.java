package net.judah.gui.settable;

import java.util.ArrayList;

import lombok.Getter;
import net.judah.api.ZoneMidi;
import net.judah.seq.track.MidiTrack;

@Getter
public class Program extends SetCombo<String> {

	private static final ArrayList<Program> instances = new ArrayList<>();

	private final MidiTrack track;
	private final ZoneMidi midiOut;
	private final int ch;

	public Program(ZoneMidi midi) {
		super(midi.getPatches(), midi.getProg(0));
		track = null;
		midiOut = midi;
		ch = 0;
		instances.add(this);
	}

	public Program(MidiTrack t) {
		super(t.getMidiOut().getPatches(), t.getMidiOut().getProg(t.getCh()));
		track = t;
		midiOut = track.getMidiOut();
		ch = track.getCh();
		instances.add(this);
	}

	@Override
	protected void action() {
		String change = getSelectedItem().toString();
		if (track == null)
			midiOut.progChange(change);
		else
			track.progChange(change);
	}

	public static void update(Program data) {

		String current = data.midiOut.getProg(data.ch);
		for (Program c : instances)
			if (c.midiOut == data.midiOut && c.ch == data.ch)
				if (false == current.equals(c.getSelectedItem()))
					c.override(current);
	}

	public static Program first(ZoneMidi synth, int ch) {
		for (Program c : instances)
			if (c.midiOut == synth && c.ch == ch)
				return c;
		return null;
	}

}
