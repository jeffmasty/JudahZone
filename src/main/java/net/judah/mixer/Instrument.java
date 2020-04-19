package net.judah.mixer;

import java.util.HashMap;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import net.judah.midi.Midi;
import net.judah.mixer.instrument.InstType;

@Data @AllArgsConstructor
public class Instrument {
	
	@NonNull protected final String name;
	@NonNull protected final InstType type; 
	protected String[] sourcePort;
	
	// Control name to Midi message map
	protected HashMap<String, Midi> controls;
	
}
