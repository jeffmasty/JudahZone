package net.judah.mixer;

import java.util.HashMap;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.judah.api.Midi;

@Data @AllArgsConstructor @RequiredArgsConstructor
public class Instrument {
	
	@NonNull protected final String name;
	@NonNull protected LineType type = LineType.OTHER; 
	protected final String[] sourcePort;
	
	// Control name to Midi message map
	protected HashMap<String, Midi> controls;
	
}
