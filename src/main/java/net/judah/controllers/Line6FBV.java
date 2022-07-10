package net.judah.controllers;

import net.judah.ControlPanel;
import net.judah.JudahZone;
import net.judah.MainFrame;
import net.judah.api.Midi;
import net.judah.looper.Loop;
import net.judah.mixer.Channel;
import net.judah.mixer.DrumTrack;
import net.judah.mixer.LineIn;
import net.judah.util.RTLogger;

public class Line6FBV implements Controller {

	private int pedalMemory = 50;
	private boolean mutes;

	@Override
	public boolean midiProcessed(Midi midi) {
		
		if (Midi.isProgChange(midi)) {
			if (midi.getData1() == 0) {
				if (Jamstik.getInstance().isActive()) {
					Jamstik.getInstance().nextMidiOut();
				}
				else {
					RTLogger.log(this, "DOWN");
				}
				return true;
			}
			if (midi.getData1() == 1) {
				if (Jamstik.getInstance().isActive()) {
					Jamstik.getInstance().nextMidiOut(); 
				}
				else {
					RTLogger.log(this, "UP");
				}
				return true;
			}
		}

		if (!Midi.isCC(midi)) return false;
		
		LineIn guitar = JudahZone.getChannels().getGuitar();
		Loop loop;
		switch (midi.getData1()) {
		case 1: // Loop A
			if (midi.getData2() == 0) return true;
			loop = JudahZone.getLooper().getLoopA();
			if (mutes) 
				loop.setOnMute(!loop.isOnMute());
			else 
				KorgPads.trigger(loop);
			return true;
		case 2: // overdub B
			if (midi.getData2() == 0) return true;
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
			if (midi.getData2() == 0) return true;
			loop = JudahZone.getLooper().getLoopC();
			if (mutes) 
				loop.setOnMute(!loop.isOnMute());
			else // can be free-style loop
				KorgPads.record(JudahZone.getLooper().getLoopC());
			return true;
		case 4: // overdub D
			if (midi.getData2() == 0) return true;
			DrumTrack drums = JudahZone.getLooper().getDrumTrack();
			if (mutes) 
				drums.setOnMute(!drums.isOnMute());
			else if (JudahZone.getLooper().getLoopB().hasRecording()) // TODO
				KorgPads.record(drums);
			else // or Toggle Drum Track recording
				drums.toggle();
			return true;
		case 5: // Func(1) turn on/off Jamstik midi 
			Jamstik.getInstance().toggle();
			return true;
		case 6: // Func(2) // TODO Preset on/off  or mute record in Synth mode
			return true;
		case 7: // TAP() // TODO A/B looper
			mutes = midi.getData2() > 0;
			return true;
		case 8: // Stomp
			guitar.getOverdrive().setActive(midi.getData2() > 0);
			return true;
		case 9: // Mod
			guitar.getChorus().setActive(midi.getData2() > 0);
			return true;
		case 10: // Delay
			guitar.getDelay().setActive(midi.getData2() > 0);
			return true;
		case 11: // Reverb
			guitar.getReverb().setActive(midi.getData2() > 0);
			return true;

		// 12 : toe switch ? 
		// 13 : pedal wah
		case 14: // pedal vol of focus channel
			int percent = (int)(midi.getData2() / 1.27f);
			if (Math.abs(pedalMemory - percent) < 2)
				return true; // ignore minor fluctuations of vol pedal
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
