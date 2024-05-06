package net.judah.controllers;

import static net.judah.JudahZone.*;

import net.judah.fx.Gain;
import net.judah.gui.MainFrame;
import net.judah.looper.Loop;
import net.judah.looper.SoloTrack;
import net.judah.midi.JudahClock;
import net.judah.midi.Midi;
import net.judah.mixer.Channel;
import net.judah.mixer.Instrument;

public class Line6FBV implements Controller {
	public static final String NAME = "FBV Shortboard Mk";
	private int pedalMemory = 50;
	private boolean mutes;

	@Override
	public boolean midiProcessed(Midi midi) {
		
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
		
		Instrument guitar = getGuitar();
		Loop loop;
		final int data2 = midi.getData2();
		switch (midi.getData1()) {
		case 1: // Loop A
			if (data2 == 0) return true;
			loop = getLooper().getLoopA();
			if (mutes) 
				loop.setOnMute(!loop.isOnMute());
			else 
				loop.trigger();
			return true;
		case 2: // overdub B
			if (data2 == 0) return true;
			loop = getLooper().getLoopB();
			if (mutes) 
				loop.setOnMute(!loop.isOnMute());
			else {
				loop.trigger();
			}
			return true;
		case 3: // record C (free)
			if (data2 == 0) return true;
			loop = getLooper().getLoopC();
			if (mutes) 
				loop.setOnMute(!loop.isOnMute());
			else // can be free-style loop
				loop.trigger();
			return true;
		case 4: // overdub D
			if (midi.getData2() == 0) return true;
			SoloTrack solo = getLooper().getSoloTrack();
			if (mutes) 
				solo.setOnMute(!solo.isOnMute());
			else
				solo.trigger();
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
