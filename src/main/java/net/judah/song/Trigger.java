package net.judah.song;

import static net.judah.gui.Pastels.*;

import java.awt.Color;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum Trigger {
	
	INIT(MY_GRAY.darker()),
	CUE(MY_GRAY), 
	LOOP(PINK),
	BAR(YELLOW),
	ABS(ORANGE), 
	REL(BLUE);
	
	@Getter private final Color color;
	@Getter private static final Color active = GREEN;
	
}