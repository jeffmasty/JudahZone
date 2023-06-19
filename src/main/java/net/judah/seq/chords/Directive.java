package net.judah.seq.chords;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor @Getter
public enum Directive {

	SCENES("{x_zone_scenes}"),
	MUTES("{x_zone_versechorus}"),
	LENGTH("{x_zone_length}"),
	LOOP("{x_zone_loop}");
	

	private final String literal;
}
