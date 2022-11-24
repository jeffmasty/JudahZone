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
		MIDICLOCK("midiclock"),
		KEYBOARD("keyboard"), 
		MIXER("mixer"),
		PADS("pads"),
		LINE6_IN("line6In"),
		BEATSTEP("beatstep"),
		JAMSTIK("jamstik");
		;

		// Old controllers
//		JUDAH_SYNTH("JudahSynth")
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
		SYNTH_OUT("Fluid"), 
		CRAVE_OUT("Crave"),
		//CLOCK_OUT("Clock"),
		//CALF_OUT("Calf"),
		//CIRCUIT_OUT("Circuit")
		//UNO_OUT("Uno"),
		//AUX1_OUT("Aux");
		;
		@Getter public final String port;
		
	}
	
}
