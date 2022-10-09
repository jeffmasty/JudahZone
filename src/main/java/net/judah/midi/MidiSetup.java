package net.judah.midi;

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
		LINE6_IN("line6In"),
		JUDAH_SYNTH("JudahSynth");
		;

		// Old controllers
		// CIRCUIT_IN("circuitIn"),
		// JAMSTIK_IN("jamstikIn"),
		// AUX_IN("auxIn"); //audioInterface
		// ARDUINO("arduino"),
		// CRAVE_IN("craveIn"),
		// PEDAL("pedal"),

		@Getter public final String port;
	}

	@RequiredArgsConstructor
	public enum OUT { // out ports
		TEMPO("Tempo"),
		CLOCK_OUT("Clock"),
		SYNTH_OUT("Fluid"), 
		CRAVE_OUT("Crave"),
		//CALF_OUT("Calf"),
		//CIRCUIT_OUT("Circuit")
		//UNO_OUT("Uno"),
		//AUX1_OUT("Aux");
		;
		@Getter public final String port;
		
	}
	
}
