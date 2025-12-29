package net.judah.synth;

import judahzone.util.Constants;
import net.judah.mixer.LineIn;

/** internal Audio generators that respond to Midi (synths and drum machines) */
public abstract class Engine extends LineIn implements ZoneMidi {

	public Engine(String name, int channels) {
		super(name, channels == Constants.STEREO);
	}

	public Engine(String name, boolean isStereo) {
		super(name, isStereo);
	}

}
