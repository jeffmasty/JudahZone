package net.judah.seq;

import java.awt.Color;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.judah.gui.Pastels;

@RequiredArgsConstructor 
public enum CUE implements Pastels {
	Hot(PINK), Bar(ORANGE), Loop(YELLOW);
	
	@Getter private final Color color;
}