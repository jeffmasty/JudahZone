package net.judah.api;

import net.judah.gui.knobs.Knobs;
import net.judah.mixer.LineIn;
import net.judah.util.Constants;

/** internal Audio generators that respond to Midi (synths and drum machines) */
public abstract class Engine extends LineIn implements ZoneMidi, Knobs {

	public Engine(String name, int channels) {
		super(name, channels == Constants.STEREO);
	}

	public Engine(String name, boolean isStereo) {
		super(name, isStereo);
	}

}
