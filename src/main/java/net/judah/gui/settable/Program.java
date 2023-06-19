package net.judah.gui.settable;

import java.util.ArrayList;

import lombok.Getter;
import net.judah.api.MidiReceiver;

public class Program extends SetCombo<String> {
	
	private static final ArrayList<Program> instances = new ArrayList<>();

	@Getter private final MidiReceiver port;
	@Getter private final int ch;
	
	public Program(MidiReceiver midi) {
		this(midi, 0);
	}
	
	public Program(MidiReceiver midi, int channel) {
		super(midi.getPatches(), midi.getProg(channel));
		port = midi;
		ch = channel;
		instances.add(this);
	}
	
	public String[] getPrograms() {
		return port.getPatches();
	}
	
	@Override
	protected void action() {
		if (set == this) return;
		port.progChange(getSelectedItem().toString(), ch);
	}

	public static void update(Program data) {
		String current = data.getPort().getProg(data.ch);
		for (Program c : instances)
			if (c.port == data.port && c.ch == data.ch)
				if (false == current.equals(c.getSelectedItem()))
					c.override(current);
	}

	public static Program first(MidiReceiver synth, int ch) {
		for (Program c : instances)
			if (c.port == synth && c.ch == ch)
				return c;
		return null;
	}

	
	
}
