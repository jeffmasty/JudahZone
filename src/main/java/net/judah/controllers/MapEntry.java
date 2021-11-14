package net.judah.controllers;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MapEntry {
	
		public static enum TYPE {
			MOMENTARY, TOGGLE, KNOB, NOTE_ON, NOTE_OFF, PROGCHAN, OTHER
	}

		@Getter private final String name;
		@Getter private final TYPE type;
		@Getter private final int val;
		
}
