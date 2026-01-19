package net.judah.synth;

import judahzone.util.Constants;
import lombok.Getter;
import net.judah.channel.LineIn;

/** internal Audio generators that respond to Midi (synths and drum machines) */
public abstract class Engine extends LineIn implements ZoneMidi {

	// PHASE II, if external, we need ports/registrations/connections
	@Getter protected boolean external;
	protected String commandLine;

	public Engine(String name, int channels) {
		super(name, channels == Constants.STEREO);
	}

	public Engine(String name, boolean isStereo) {
		super(name, isStereo);
	}

}
