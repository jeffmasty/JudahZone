package net.judah.controllers;

import static net.judah.JudahZone.getFxRack;
import static net.judah.JudahZone.getGuitar;
import static net.judah.JudahZone.getMixer;

import lombok.Getter;
import net.judah.fx.Gain;
import net.judah.gui.Pastels;
import net.judah.gui.widgets.MidiPatch;
import net.judah.midi.JudahMidi;
import net.judah.midi.Midi;
import net.judah.midi.Panic;
import net.judah.mixer.LineIn;
import net.judah.util.Constants;

/** reroute guitar midi to synths */
public class Jamstik extends MidiPatch implements Controller {
	
	@Getter private boolean active;
	private int volStash = 50;
	
	public void setActive(boolean active) {
		this.active = active;
		LineIn guitar = getGuitar();
		if (active) {
			volStash = guitar.getVolume();
			guitar.getGain().setGain(0);
		} else {
			guitar.getGain().set(Gain.VOLUME, volStash);
			new Panic(track);
		}
		Constants.execute(() ->{
			if (frame != null)
				frame.setBackground(active ? Pastels.GREEN : null);		
			getMixer().update(guitar);
			if (getFxRack().getChannel() == getGuitar())
				getFxRack().getChannel().getGui().update();
		});

	}
	
	public void toggle() {
		setActive(!active);
	}

	@Override
	public boolean midiProcessed(Midi midi) {
		if (active && Midi.isNote(midi))
			send(midi, JudahMidi.ticker());
		return true;
	}
	
}
