package net.judah.controllers;

import static net.judah.JudahZone.getFxRack;
import static net.judah.JudahZone.getGuitar;
import static net.judah.JudahZone.getMixer;

import javax.swing.JToggleButton;

import lombok.Getter;
import lombok.Setter;
import net.judah.api.Key;
import net.judah.fx.Gain;
import net.judah.gui.MainFrame;
import net.judah.gui.Updateable;
import net.judah.midi.JudahMidi;
import net.judah.midi.Midi;
import net.judah.midi.Panic;
import net.judah.mixer.Channel;
import net.judah.mixer.LineIn;

/** reroute guitar midi to synths, w/ optional octaver */
public class Jamstik implements Controller, Updateable {

	@Getter private boolean active;
	private int volStash = 50;
	@Setter @Getter private boolean octaver;

	@Getter private final JToggleButton toggleBtn = new JToggleButton("010");
	@Getter private final JToggleButton octBtn = new JToggleButton(" ↓ ");

	public Jamstik() {
		toggleBtn.addActionListener(l->setActive(toggleBtn.isSelected()));
		toggleBtn.setToolTipText("Jamstik Midi Guitar");
		octBtn.addActionListener(l->setOctaver(octBtn.isSelected()));
		octBtn.setToolTipText("Octaver On/Off");
	}

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
		MainFrame.update(this);
	}

	public void toggle() {
		setActive(!active);
	}

	public void octaver() {
		octaver = !octaver;
		if (octaver && !active)
			toggle();
		else
			MainFrame.update(this);
	}

	@Override public boolean midiProcessed(Midi midi) {
		if (active && Midi.isNote(midi)) // TODO pitchbend
			if (octaver)
				MPKmini.instance.getMidiOut().send(Midi.create(midi.getCommand(), 0, midi.getData1() - Key.OCTAVE,
						midi.getData2()), JudahMidi.ticker());
			else
				MPKmini.instance.getMidiOut().send(midi, JudahMidi.ticker());
		return true;
	}

	@Override public void update() {
		if (active != toggleBtn.isSelected())
			toggleBtn.setSelected(active);
		if (octaver != octBtn.isSelected())
			octBtn.setSelected(octaver);
		Channel guitar = getGuitar();
		getMixer().update(guitar);
		if (getFxRack().getChannel() == guitar)
			MainFrame.update(guitar);
	}

}
