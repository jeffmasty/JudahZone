package net.judah.settings;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** My Setup */
public class MidiSetup {

	public static final int SYNTH_CHANNEL = 0;
	public static final int DRUMS_CHANNEL = 9;

	@RequiredArgsConstructor
	public static enum IN { 
		// in Midi ports
		KEYBOARD("keyboard"), 
		MIXER("mixer"),
		PADS("pads"),
		CIRCUIT_IN("circuitIn"),
		LINE6_IN("line6In"),
		// JAMSTIK_IN("jamstikIn"),
		AUX_IN("auxIn"); //audioInterface

		// Old controllers
		//	ARDUINO("arduino"),
		//	CRAVE_IN("craveIn"),
		//	PEDAL("pedal"),

		@Getter public final String port;
	}

	@RequiredArgsConstructor
	public enum OUT { // out ports
		CLOCK_OUT("clockOut"),
		SYNTH_OUT("synthOut"), 
		CALF_OUT("calfOut"),
		CRAVE_OUT("craveOut"),
		UNO_OUT("unoOut"),
		CIRCUIT_OUT("circuitOut"),
		AUX1_OUT("auxOut");
		
		@Getter public final String port;
		
	}
	
}
