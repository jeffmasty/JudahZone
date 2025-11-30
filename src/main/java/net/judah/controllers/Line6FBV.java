package net.judah.controllers;

import static net.judah.JudahZone.*;

import net.judah.fx.Gain;
import net.judah.gui.MainFrame;
import net.judah.looper.Loop;
import net.judah.midi.JudahClock;
import net.judah.midi.Midi;
import net.judah.mixer.Channel;
import net.judah.mixer.LineIn;
import net.judah.util.Debounce;

public class Line6FBV extends Debounce implements Controller {
	public static final String NAME = "FBV Shortboard Mk";
	private int pedalMemory = 50;
	private boolean mutes;
	private final LineIn guitar;

	public Line6FBV(LineIn guitar) {
		this.guitar = guitar;
	}

	@Override public boolean midiProcessed(Midi midi) {

		if (Midi.isProgChange(midi)) { // "DOWN" button
			if (midi.getData1() == 0) {
				getLooper().verseChorus(); // toggle mutes
				return true;
			}
			if (midi.getData1() == 1) { // "UP" button
				getOverview().trigger();
				return true;
			}
		}

		if (!Midi.isCC(midi)) return false;

		if (doubleTap(this))
			return true;

		final int data2 = midi.getData2();
		switch (midi.getData1()) {
		case 1: // record or mute loops
		case 2:
		case 3:
		case 4:
			if (data2 == 0) return true;
			Loop loop = getLooper().get(midi.getData1() - 1);
			if (mutes)
				loop.toggleMute();
			else
				getLooper().trigger(loop);
			return true;
		case 5: // Func(1) start/stop drum machine
			JudahClock clock = getClock();
			if (clock.isActive())
				clock.end();
			else
				clock.begin();
			return true;
		case 6: // Func(2) turn on/off Jamstik midi
			if (mutes)
				getMidi().getJamstik().octaver();
			else
				getMidi().getJamstik().toggle();
			return true;
		case 7: // TAP()   toggle record vs. mute controls
			mutes = data2 > 0;
			return true;
		case 8: // Stomp
			guitar.getOverdrive().setActive(data2 > 0);
			MainFrame.update(guitar);
			return true;
		case 9: // Mod
			guitar.getChorus().setActive(data2 > 0);
			MainFrame.update(guitar);
			return true;
		case 10: // Delay
			guitar.getDelay().setActive(data2 > 0);
			MainFrame.update(guitar);
			return true;
		case 11: // Reverb
			MainFrame.update(guitar);
			guitar.getReverb().setActive(data2 > 0);
			return true;

		// 12 : toe switch ?
		// 13 : pedal wah

		case 14: // pedal vol of focus channel
			int percent = (int)(data2 / 1.27f);
			if (Math.abs(pedalMemory - percent) < 2)
				return true; // ignore minor fluctuations of vol pedal
			if (getFxRack() == null) // startup?
				return true;

			Channel ch = getFxRack().getChannel();
			if (ch == null) return true;
			if (Math.abs(ch.getVolume() - percent) > 3){
				ch.getGain().set(Gain.VOLUME, percent);
				pedalMemory = percent;
				MainFrame.update(ch);
			}
			return true;
		}
		return false;
	}

}
