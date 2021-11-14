package net.judah.plugin;

import net.judah.JudahZone;
import net.judah.api.AudioMode;
import net.judah.api.Midi;
import net.judah.controllers.Controller;
import net.judah.sequencer.Sequencer;
import net.judah.util.RTLogger;

public class ArduinoPedal implements Controller {

	@Override
	public boolean midiProcessed(Midi midi) {
		RTLogger.log(this, midi.toString());
		if (midi.isCC()) {
			if (midi.getData1() == 1) {
				Sequencer.trigger();
				return true;
			}
			else if (midi.getData1() == 2) {
				JudahZone.getLooper().getLoopB().record(
						JudahZone.getLooper().getLoopB().isRecording() != AudioMode.RUNNING);
				return true;
			}
		}
		return false;
	}

}
