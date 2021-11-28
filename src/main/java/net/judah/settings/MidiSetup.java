package net.judah.settings;

import lombok.RequiredArgsConstructor;

/** My Setup */
public class MidiSetup {

	public static final int SYNTH_CHANNEL = 0;
	public static final int DRUMS_CHANNEL = 9;
	public static final int AUX_CHANNEL = 6;

	@RequiredArgsConstructor
	public static enum IN { // in ports
		KEYBOARD("keyboard"), 
		PEDAL("pedal"),
		MIXER("mixer"),
		PADS("pads"),
		ARDUINO("arduino"),
		DRUMS_IN("drumsIn"),
		AUX_IN("auxIn"); //AUX1_IN("audioInterface"),
		
		public final String name;
	}

	@RequiredArgsConstructor
	public enum OUT { // out ports
		SYNTH_OUT("synthOut"), 
		DRUMS_OUT("drumsOut"), 
		CALF_OUT("calfOut"),
		AUX1_OUT("auxOut");
		
//		SYNTH_OUT("synthOut", AUX_CHANNEL), 
//		DRUMS_OUT("drumsOut", DRUMS_CHANNEL), 
//		AUX1_OUT("aux1out", AUX_CHANNEL),
//		AUX2_OUT("aux2out", 10),
//		AUX3_OUT("aux3out", 11);
		
		public final String name;
//		public final int channel;
		
//		public static int getChannel(String port) {
//			for (OUT val : values())
//				if (val.name.equals(port))
//					return val.channel;
//			return 0;
//		}
		
	}
	
}
