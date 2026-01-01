package net.judah.seq.track;

import java.awt.Color;

import judahzone.gui.Pastels;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor 
public enum Cue implements Pastels {
	Hot(PINK), Bar(ORANGE), Loop(YELLOW);
	
	@Getter private final Color color;
}
