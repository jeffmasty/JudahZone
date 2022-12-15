package net.judah.seq;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor @Getter
public enum Gate {

	
	SIXTEENTH("1/16"), 
	EIGHTH("1/8"), 
	QUARTER("1/4"), 
	NONE("FREE"),
	MICRO("1/32"),
	HALF("1/2"),
	WHOLE("[0]"),
	RATCHET("TRILL");
	
	private final String name;
	@Override
	public String toString() {
		return name;
	}
}
	
