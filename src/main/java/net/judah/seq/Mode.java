package net.judah.seq;

import java.awt.Color;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.judah.gui.Pastels;

@RequiredArgsConstructor @Getter
public enum Mode implements Pastels {
	Off(GREEN), 
	// MPK(ORANGE), 
	// REC(RED),
	BASS(Color.BLUE),
	CHRD(Color.CYAN),
//	ABS(YELLOW),
//	REL(PINK),
	UP(Color.GRAY), 
	DWN(Color.GRAY), 
	UPDN(Color.GRAY),
	RND(Color.MAGENTA), 
	RACM(Color.MAGENTA), 
	// SCL1(MAGENTA),
	// SCL2(MAGENTA),
	;
	
	private final Color color;
}
