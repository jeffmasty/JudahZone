package net.judah.mixer;

import com.sun.istack.internal.NotNull;

import lombok.Data;

@Data
public class Instrument {
	public static enum Type {
		Sys, Synth, Other
	}
	
	@NotNull private final String name;
	@NotNull private final Type type; 
	private String[] sourcePort;
	
}
