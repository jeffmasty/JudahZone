package net.judah.gui.settable;

import java.util.ArrayList;

import lombok.Getter;
import net.judah.api.ZoneMidi;
import net.judah.seq.track.MidiTrack;

public class Program extends SetCombo<String> {
	
	private static final ArrayList<Program> instances = new ArrayList<>();

	private final MidiTrack track;
	@Getter private final ZoneMidi port;
	private final int ch;

	public Program(MidiTrack t) {
		super(t.getMidiOut().getPatches(), t.getMidiOut().getProg(t.getCh()));
		track = t;
		port = track.getMidiOut();
		ch = track.getCh();
		instances.add(this);
	}
	
	@Override
	protected void action() {
		track.progChange(getSelectedItem().toString());
	}
	
	public static void update(Program data) {
		
		String current = data.port.getProg(data.ch);
		for (Program c : instances)
			if (c.port == data.port && c.ch == data.ch)
				if (false == current.equals(c.getSelectedItem()))
					c.override(current);
	}

	public static Program first(ZoneMidi synth, int ch) {
		for (Program c : instances)
			if (c.port == synth && c.ch == ch)
				return c;
		return null;
	}
	
}
