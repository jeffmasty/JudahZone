package net.judah.controllers;

import net.judah.JudahZone;
import net.judah.MainFrame;
import net.judah.api.Midi;
import net.judah.effects.gui.ControlPanel;
import net.judah.looper.Loop;
import net.judah.mixer.Channel;
import net.judah.mixer.Instrument;
import net.judah.mixer.SoloTrack;

public class Line6FBV implements Controller {

	private int pedalMemory = 50;
	private boolean mutes;

	@Override
	public boolean midiProcessed(Midi midi) {
		
		if (Midi.isProgChange(midi)) { // "DOWN" button
			if (midi.getData1() == 0) { 
				JudahZone.getLooper().verseChorus();
				return true;
			}
			if (midi.getData1() == 1) { // "UP" button
				Jamstik.nextMidiOut(); 
				return true;
			}
		}

		if (!Midi.isCC(midi)) return false;
		
		Instrument guitar = JudahZone.getChannels().getGuitar();
		Loop loop;
		final int data2 = midi.getData2();
		switch (midi.getData1()) {
		case 1: // Loop A
			if (data2 == 0) return true;
			loop = JudahZone.getLooper().getLoopA();
			if (mutes) 
				loop.setOnMute(!loop.isOnMute());
			else 
				KorgPads.trigger(loop);
			return true;
		case 2: // overdub B
			if (data2 == 0) return true;
			loop = JudahZone.getLooper().getLoopB();
			if (mutes) 
				loop.setOnMute(!loop.isOnMute());
			else {
				if (loop.hasRecording())
					KorgPads.record(JudahZone.getLooper().getLoopB());
				else { // or Sync B
					loop.setArmed(!loop.isArmed());
					MainFrame.update(loop);
				}
			}
			return true;
		case 3: // record C (free)
			if (data2 == 0) return true;
			loop = JudahZone.getLooper().getLoopC();
			if (mutes) 
				loop.setOnMute(!loop.isOnMute());
			else // can be free-style loop
				KorgPads.record(JudahZone.getLooper().getLoopC());
			return true;
		case 4: // overdub D
			if (midi.getData2() == 0) return true;
			SoloTrack drums = JudahZone.getLooper().getDrumTrack();
			if (mutes) 
				drums.setOnMute(!drums.isOnMute());
			else if (JudahZone.getLooper().getLoopB().hasRecording()) // TODO
				KorgPads.record(drums);
			else // or Toggle Drum Track recording
				drums.toggle();
			return true;
		case 5: // Func(1) turn on/off Jamstik midi 
			Jamstik.toggle();
			return true;
		case 6: // Func(2) // Preset on/off  or mute record in Synth mode
			guitar.setPresetActive(data2 > 0);
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
			if (ControlPanel.getInstance() == null)
				return true;
			Channel ch = ControlPanel.getInstance().getCurrent().getChannel();
			if (Math.abs(ch.getGain().getVol() - percent) > 2){
				ch.getGain().setVol(percent);
				pedalMemory = percent;
				MainFrame.update(ch);
			}
			return true;
		}		
		return false;
	}

}
