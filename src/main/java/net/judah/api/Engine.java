package net.judah.api;

import java.util.Vector;

import lombok.Getter;
import net.judah.gui.knobs.Knobs;
import net.judah.mixer.LineIn;
import net.judah.seq.track.MidiTrack;

/** internal Sound generators that respond to Midi (synths, drum machines) */
public abstract class Engine extends LineIn implements MidiReceiver, Knobs {

	@Getter protected Vector<MidiTrack> tracks = new Vector<>();
	
	public Engine(String name, boolean isStereo) {
		super(name, isStereo);
	}
	
	public abstract void process();
}
