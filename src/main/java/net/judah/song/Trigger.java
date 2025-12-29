package net.judah.song;

import static net.judahzone.gui.Pastels.ORANGE;
import static net.judahzone.gui.Pastels.PINK;
import static net.judahzone.gui.Pastels.YELLOW;

import java.awt.Color;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum Trigger {

	BAR(ORANGE),
	LOOP(YELLOW),
	HOT(PINK)
	;

	@Getter private final Color color;
}