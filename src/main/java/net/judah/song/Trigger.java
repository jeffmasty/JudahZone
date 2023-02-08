package net.judah.song;

import static net.judah.gui.Pastels.*;

import java.awt.Color;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum Trigger {
	
	BAR(ORANGE),
	LOOP(YELLOW),
	HOT(PINK), 
	ABS(RED), 
	REL(BLUE)
	;
//	JUMP(PURPLE);
	
	@Getter private final Color color;
	
}