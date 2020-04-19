package net.judah.mixer;

import java.util.HashMap;

import lombok.Data;
import lombok.NonNull;
import net.judah.midi.Midi;

@Data
public abstract class Instrument {
	public static enum Type {
		Sys, Synth, Other
	}
	
	@NonNull protected final String name;
	@NonNull protected final Type type; 
	protected String[] sourcePort;
	
	// Control name to Midi message map
	protected HashMap<String, Midi> controls;
	
}
