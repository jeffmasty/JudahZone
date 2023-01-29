package net.judah.song;

import static net.judah.gui.Pastels.*;

import java.awt.Color;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum Trigger {
	
	BAR(YELLOW),
	LOOP(PINK),
	HOT(PINK), 
	ABS(RED), 
	REL(ORANGE),
	JUMP(PURPLE);
	
	@Getter private final Color color;
	@Getter private static final Color active = GREEN;
	
}