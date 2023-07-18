package net.judah.gui.settable;

import java.util.ArrayList;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.api.MidiReceiver;
import net.judah.seq.track.MidiTrack;

public class Program extends SetCombo<String> {
	
	private static final ArrayList<Program> instances = new ArrayList<>();

	@Getter private final MidiReceiver port;
	@Getter private final int ch;
	private MidiTrack track;

	public Program(MidiTrack t) {
		this(t.getMidiOut(), t.getCh());
		track = t;
	}
	
	public Program(MidiReceiver rcv, int channel) {
		super(rcv.getPatches(), rcv.getProg(channel));
		this.port = rcv;
		this.ch = channel;
		instances.add(this);
	}
	
	@Override
	protected void action() {
		if (set == this) return;
		getTrack().progChange(getSelectedItem().toString());
	}

	public MidiTrack getTrack() {
		if (track == null)
			track = JudahZone.getSeq().lookup(port, ch);
		return track;
	}
	
	public static void update(Program data) {
		
		String current = data.port.getProg(data.ch);
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
