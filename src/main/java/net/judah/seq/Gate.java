package net.judah.seq;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor @Getter
public enum Gate {

	NONE("Free"),
	SIXTEENTH("1/16"), 
	EIGHTH("1/8"), 
	QUARTER("1/4"), 
	HALF("1/2"),
	WHOLE("[0]"),
	MICRO("1/32"),
	//RATCHET("TRILL"),
	FILE("File");
	
	private final String name;
	@Override
	public String toString() {
		return name;
	}
}
	
