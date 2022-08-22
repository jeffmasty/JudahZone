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
		PULSE("pulse"),
		KEYBOARD("keyboard"), 
		MIXER("mixer"),
		PADS("pads"),
		CIRCUIT_IN("circuitIn"),
		LINE6_IN("line6In"),
		;
		// JAMSTIK_IN("jamstikIn"),
		// AUX_IN("auxIn"); //audioInterface

		// Old controllers
		//	ARDUINO("arduino"),
		//	CRAVE_IN("craveIn"),
		//	PEDAL("pedal"),

		@Getter public final String port;
	}

	@RequiredArgsConstructor
	public enum OUT { // out ports
		TEMPO("Tempo"),
		CLOCK_OUT("Clock"),
		SYNTH_OUT("Fluid"), 
		CALF_OUT("Calf"),
		CRAVE_OUT("Crave"),
		CIRCUIT_OUT("Circuit")
		//UNO_OUT("Uno"),
		;//AUX1_OUT("Aux");
		
		@Getter public final String port;
		
	}
	
}
