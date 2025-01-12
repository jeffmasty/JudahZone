package net.judah.controllers;

import static net.judah.JudahZone.getFxRack;
import static net.judah.JudahZone.getGuitar;
import static net.judah.JudahZone.getMixer;

import lombok.Getter;
import net.judah.api.Key;
import net.judah.fx.Gain;
import net.judah.gui.MainFrame;
import net.judah.midi.JudahMidi;
import net.judah.midi.Midi;
import net.judah.midi.Panic;
import net.judah.mixer.LineIn;
import net.judah.omni.Threads;

/** reroute guitar midi to synths */
public class Jamstik implements Controller {

	@Getter private boolean active;
	private int volStash = 50;
	public boolean octaver;

	public void setActive(boolean active) {
		this.active = active;
		LineIn guitar = getGuitar();
		if (active) {
			volStash = guitar.getVolume();
			guitar.getGain().setGain(0);
		} else {
			guitar.getGain().set(Gain.VOLUME, volStash);
			new Panic(MPKmini.instance.getMidiOut());
		}
		Threads.execute(() ->{
			getMixer().update(guitar);
			if (getFxRack().getChannel() == getGuitar())
				MainFrame.update(guitar);
		});

	}

	public void toggle() {
		setActive(!active);
	}

	public void octaver() {
		octaver = !octaver;
		if (octaver && !active)
			toggle();
	}

	@Override
	public boolean midiProcessed(Midi midi) {
		if (active && Midi.isNote(midi)) // TODO pitchbend
			if (octaver)
				MPKmini.instance.getMidiOut().send(Midi.create(midi.getCommand(), 0, midi.getData1() - Key.OCTAVE,
						midi.getData2()), JudahMidi.ticker());
			else
				MPKmini.instance.getMidiOut().send(midi, JudahMidi.ticker());
		return true;
	}

}
