package net.judah.seq.arp;

import static java.awt.Color.BLUE;
import static java.awt.Color.CYAN;
import static java.awt.Color.GRAY;
import static java.awt.Color.MAGENTA;
import static java.awt.Color.ORANGE;
import static java.awt.Color.PINK;
import static java.awt.Color.YELLOW;

import java.awt.Color;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor @Getter
public enum Mode { 
	Off(null), 
	BASS(BLUE),
	CHRD(CYAN),
	CHOP(CYAN),
	ETH(CYAN),
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
	// UP5
	// DN5
	// CHOP
	// SCL1(MAGENTA),
	// SCL2(MAGENTA),
	;
	
	private final Color color;
}
