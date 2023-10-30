package net.judah.seq.arp;

import static java.awt.Color.*;

import java.awt.Color;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor @Getter
public enum Mode { 
	Off(null), 
	BASS(BLUE),
	CHRD(CYAN),
	MPK(ORANGE), 
//	REC(RED),
	ABS(YELLOW),
	// REL(PINK),
	UP(GRAY), 
	DWN(GRAY), 
	UPDN(GRAY),
	DNUP(GRAY),
	RND(MAGENTA), 
	RACM(PINK),
	ETH(CYAN),
	// UP5
	// DN5
	// SCL1(MAGENTA),
	// SCL2(MAGENTA),
	;
	
	private final Color color;
}
