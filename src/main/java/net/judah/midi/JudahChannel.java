package net.judah.midi;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor 
public enum JudahChannel {

	SYNTH(MidiClient.SYNTH_CHANNEL), DRUMS(MidiClient.DRUMS_CHANNEL), AUX(MidiClient.AUX_CHANNEL), KOMPLETE(MidiClient.KOMPLETE_CHANNEL); 
	
	@Getter private final int channel;
	
}
