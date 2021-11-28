package net.judah.controllers;

import net.judah.JudahZone;
import net.judah.api.AudioMode;
import net.judah.api.Midi;
import net.judah.clock.JudahClock;
import net.judah.looper.Recorder;
import net.judah.sequencer.Sequencer;
import net.judah.util.RTLogger;

public class ArduinoPedal implements Controller {

	@Override public boolean midiProcessed(Midi midi) {
		RTLogger.log(this, midi.toString());
		if (midi.isCC()) {
			if (midi.getData1() == 1) {
				Sequencer.trigger();
				return true;
			}
			else if (midi.getData1() == 2) {
				Recorder b = JudahZone.getLooper().getLoopB();
				if (JudahClock.waiting(b))
					return true;
				b.record(b.isRecording() != AudioMode.RUNNING);
				return true;
			}
		}
		return false;
	}

}
