package net.judah.seq.track;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter @RequiredArgsConstructor
public enum Cycle {
	A("Loop 1 bar", 1),
	AB("Loop 2 bars", 2), 
	ABCD("Loop 4 bars", 4), 
	A3B("Turnaround on bar 4", 4),
	TO8("8 bar pattern", 8),
	TO12("12 bar blues", 12),
	TO16("16 bar pattern", 16),
	ALL("Loop all bars", Integer.MAX_VALUE),
	CLCK("Clock/Looper length", -1)
	//breaks?: ABC_, TOC_, TOF_,
	;
	
	private final String tooltip;
	private final int length;
	
	
}
