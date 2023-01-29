package net.judah.midi;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum Signature {

	FOURFOUR("4/4", 16, 4), SWING("Swing", 12, 3), WALTZ("Waltz", 9, 3), FIVEFOUR("5/4", 20, 4), STEP32("32x", 32, 8); 
	
	public final String name;
	public final int steps;
	public final int div;
	
	@Override
	public String toString() {
		return name;
	}
}
