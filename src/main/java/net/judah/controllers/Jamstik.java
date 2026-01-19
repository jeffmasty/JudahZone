package net.judah.controllers;

import javax.swing.JToggleButton;

import judahzone.api.Controller;
import judahzone.api.Key;
import judahzone.api.Midi;
import judahzone.fx.Gain;
import judahzone.gui.Updateable;
import lombok.Getter;
import lombok.Setter;
import net.judah.JudahZone;
import net.judah.channel.Channel;
import net.judah.channel.LineIn;
import net.judah.gui.MainFrame;
import net.judah.midi.JudahMidi;
import net.judah.midi.Panic;

/** reroute guitar midi to synths, w/ optional octaver */
public class Jamstik implements Controller, Updateable {

	@Getter private boolean active;
	private int volStash = 50;
	@Setter @Getter private boolean octaver;

	@Getter private final JToggleButton toggleBtn = new JToggleButton("010");
	@Getter private final JToggleButton octBtn = new JToggleButton(" ↓ ");

	private final JudahZone zone;
	private final MPKmini mini;

	public Jamstik(JudahZone judahZone) {
		this.zone = judahZone;
		this.mini = zone.getMpkMini();
		toggleBtn.addActionListener(l->setActive(toggleBtn.isSelected()));
		toggleBtn.setToolTipText("Jamstik Midi Guitar");
		octBtn.addActionListener(l->setOctaver(octBtn.isSelected()));
		octBtn.setToolTipText("Octaver On/Off");
	}

	public void setActive(boolean active) {
		this.active = active;
		MainFrame.update(this);

		LineIn guitar = zone.getChannels().getGuitar();
		if (guitar == null)
			return;
		if (active) {
			volStash = guitar.getVolume();
			guitar.getGain().setGain(0);
		} else {
			guitar.getGain().set(Gain.VOLUME, volStash);
			new Panic(mini.getMidiOut());
		}
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
		if (active && Midi.isNote(midi)) {// TODO pitchbend
			Midi out = Midi.create(midi.getCommand(), 0,
					midi.getData1() - (octaver ? Key.OCTAVE : 0), midi.getData2());
			mini.getMidiOut().send(out, JudahMidi.ticker());
		}
		return true;
	}

	@Override public void update() {
		if (active != toggleBtn.isSelected())
			toggleBtn.setSelected(active);
		if (octaver != octBtn.isSelected())
			octBtn.setSelected(octaver);
		Channel guitar = zone.getChannels().getGuitar();
		if (guitar == null)
			return;
		zone.getMixer().update(guitar);
		if (zone.getFxRack().getChannel() == guitar)
			MainFrame.update(guitar);
	}

}
