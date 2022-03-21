package net.judah.settings;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** My Setup */
public class MidiSetup {

	public static final int SYNTH_CHANNEL = 0;
	public static final int DRUMS_CHANNEL = 9;

	@RequiredArgsConstructor
	public static enum IN { // in ports
		KEYBOARD("keyboard"), 
		PEDAL("pedal"),
		MIXER("mixer"),
		PADS("pads"),
		ARDUINO("arduino"),
		CRAVE_IN("craveIn"),
		DRUMS_IN("drumsIn"),
		AUX_IN("auxIn"); //AUX1_IN("audioInterface"),
		
		@Getter public final String port;
	}

	@RequiredArgsConstructor
	public enum OUT { // out ports
		SYNTH_OUT("synthOut"), 
		DRUMS_OUT("drumsOut"), 
		CALF_OUT("calfOut"),
		CRAVE_OUT("craveOut"),
		AUX1_OUT("auxOut"),
		CLOCK_OUT("clockOut");
		
		@Getter public final String port;
		
	}
	
}
