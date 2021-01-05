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
		MIDI_IN("midiIn");
		public final String name;
	}

	@RequiredArgsConstructor
	public enum OUT { // out ports
		
		SYNTH("synth", AUX_CHANNEL), 
		DRUMS("drums", DRUMS_CHANNEL), 
		AUX("aux", AUX_CHANNEL), 
		;
		
		public final String name;
		public final int channel;
		
		public static int getChannel(String port) {
			for (OUT val : values())
				if (val.name.equals(port))
					return val.channel;
			return 0;
		}
		
	}
	
}
