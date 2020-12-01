package net.judah.mixer;

import java.util.HashMap;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.judah.midi.Midi;

@Data @AllArgsConstructor @RequiredArgsConstructor
public class Instrument {
	
	@NonNull protected final String name;
	@NonNull protected Channel.Type type = Channel.Type.OTHER; 
	protected final String[] sourcePort;
	
	// Control name to Midi message map
	protected HashMap<String, Midi> controls;
	
}
