package net.judah.controllers;

import net.judah.JudahZone;
import net.judah.api.Midi;
import net.judah.util.RTLogger;

public class ArduinoPedal implements Controller {

	@Override public boolean midiProcessed(Midi midi) {
		RTLogger.log(this, midi.toString());
		if (midi.isCC()) {
			if (midi.getData1() == 1) {
				KorgPads.trigger(JudahZone.getLooper().getLoopA());
				return true;
			}
			else if (midi.getData1() == 2) {
				KorgPads.record(JudahZone.getLooper().getLoopB());
				return true;
			}
		}
		return false;
	}

}
