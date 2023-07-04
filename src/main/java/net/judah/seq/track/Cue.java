package net.judah.seq.track;

import java.awt.Color;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.judah.gui.Pastels;

@RequiredArgsConstructor 
public enum Cue implements Pastels {
	Hot(PINK), Bar(ORANGE), Loop(YELLOW);
	
	@Getter private final Color color;
}
