package net.judah.gui;

import java.awt.Color;

public interface Pastels {

    Color RED = new Color(0xff6e8d);
    Color GREEN = new Color(0x90da6a);
    Color BLUE = new Color(189, 230, 250); //  98dafd // a4b9cb
    Color PINK = new Color(0xf4a2f9);
    Color PURPLE = new Color(0x9d87c1).brighter(); // 966FD6 //7955b5
    Color ORANGE = new Color(0xFFA500); // ffdf9e
    Color YELLOW = new Color(0xFFFF80);
    Color FADED = new Color(8, 8, 8, 25);

    Color EGGSHELL = new Color(252,252,246);
	Color BUTTONS = new Color(237, 237, 229);
	Color MY_GRAY = new Color(220, 220, 210);
	Color ONTAPE = GREEN;

	Color DOWNBEAT = alpha(BLUE, 90);
	Color SHADE = BUTTONS;
	Color GRID = MY_GRAY;
	Color SELECTED = ORANGE;
	Color CC = BLUE;
	Color PROGCHANGE = PINK;

	public static Color alpha(Color input, int alpha) {
		return new Color(input.getRed(), input.getGreen(), input.getBlue(), alpha);
	}
}

