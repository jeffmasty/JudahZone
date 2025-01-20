package net.judah.gui;

import java.awt.Color;

public interface Pastels {

	int GRADIENTS = 32;

    Color RED = new Color(0xff6e8d);
    Color GREEN = new Color(0x90da6a);
    Color BLUE = new Color(189, 230, 250); //  98dafd // a4b9cb
    Color PINK = new Color(0xf4a2f9);
    Color PURPLE = new Color(0x966FD6); //7955b5
    Color ORANGE = new Color(0xFFA500); // ffdf9e
    Color YELLOW = new Color(0xFFFF80);
    Color FADED = new Color(8, 8, 8, 25);

    Color EGGSHELL = new Color(252,252,246);
	Color BUTTONS = new Color(237, 237, 229);
	Color MY_GRAY = new Color(220, 220, 210);
	Color ONTAPE = GREEN;

	Color DOWNBEAT = alpha(BLUE, 90);// BLUE faded
	Color SHADE = BUTTONS;
	Color GRID = MY_GRAY;
	Color SELECTED = ORANGE;

	public static Color alpha(Color input, int alpha) {
		return new Color(input.getRed(), input.getGreen(), input.getBlue(), alpha);
	}

//	public static Color velocityColor(int data2) {
//    	return new Color(0, 112, 60, data2 * 2); // Dartmouth Green
//	}
//
//    public static Color highlightColor(int data2) {
//    	return new Color(0xFF, 0xA5, 0x00, data2 * 2); // Orange
//    }

}

