package net.judah.midi;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** My setup */
@Getter @RequiredArgsConstructor
public enum JudahPort {
	
	SYNTH("synth", MidiClient.AUX_CHANNEL), 
	DRUMS("drums", MidiClient.DRUMS_CHANNEL), 
	AUX("aux", MidiClient.AUX_CHANNEL), 
	KOMPLETE("komplete", MidiClient.KOMPLETE_CHANNEL);
	
	private final String portName;
	private final int channel;
	
	public static int getChannel(String port) {
		for (JudahPort val : values())
			if (val.getPortName().equals(port))
				return val.getChannel();
		return 0;
	}
	
}
