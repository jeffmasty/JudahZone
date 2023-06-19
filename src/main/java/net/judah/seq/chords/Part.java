package net.judah.seq.chords;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**  start_of_chorus (short: soc), end_of_chorus (short: eoc)
	start_of_verse (short: sov), end_of_verse (short: eov)
	start_of_bridge (short: sob), end_of_bridge (short: eob)	 */
@RequiredArgsConstructor @Getter
public enum Part {
	MAIN("main", 'm'),
	INTRO("intro", 'i'), 
	VERSE("verse", 'v'), 
	CHORUS("chorus", 'c'), 
	BRIDGE("bridge", 'b'), 
	ENDING("ending", 'e'), 
	OTHER("", '?')
	;
	
	final String literal;
	final char abrev;
	
	public static Part parse(String type) {
		for (Part p : values())
			if (p.literal.equals(type))
				return p;
		return MAIN;
	}
	
	public static Part parse(char type) {
		for (Part p : values())
			if (p.abrev == type)
				return p;
		return MAIN;
	}
}
