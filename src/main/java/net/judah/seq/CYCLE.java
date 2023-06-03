package net.judah.seq;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum CYCLE {
	A("Loop 1 bar"),
	AB("Loop 2 bars"), 
	ABCD("Loop 4 bars"), 
	A3B("Turnaround on bar 4"),
	BAR12("12 bar blues"),
	ALL("Loop all bars") 
	;
	@Getter private final String tooltip;
	
}
