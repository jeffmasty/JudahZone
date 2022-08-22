package net.judah.util;

import java.awt.Color;
import java.awt.Paint;

import javax.sound.midi.ShortMessage;

import net.judah.tracker.Notes;

public interface Pastels {
    Color RED = new Color(0xff6e8d);
    Color GREEN = new Color(0x90da6a);
    Color BLUE = new Color(0xbde6fa); //  98dafd // a4b9cb
    Color PINK = new Color(0xf4a2f9);
    Color PURPLE = new Color(0x966FD6); // 62b4e2
    Color ORANGE = new Color(0xFFA500); // ffdf9e
    Color YELLOW = new Color(0xFFFF80);
    
    Color EGGSHELL = new Color(252,252,246);
	Color BUTTONS = new Color(238, 238, 230);
	Color MY_GRAY = new Color(220, 220, 210);

	static Paint forType(Notes n) {
		if (n == null) return Color.WHITE;
		int cmd = n.get().getCommand();
		if (ShortMessage.NOTE_ON == cmd)
			return PINK;
		if (ShortMessage.NOTE_OFF == cmd)
			return Color.GRAY;
		return Color.WHITE;
	}

}

