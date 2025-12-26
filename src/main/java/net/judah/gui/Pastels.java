package net.judah.gui;

import java.awt.Color;

import net.judah.fx.Chorus;
import net.judah.fx.Compressor;
import net.judah.fx.Convolution;
import net.judah.fx.Delay;
import net.judah.fx.EQ;
import net.judah.fx.Effect;
import net.judah.fx.Filter;
import net.judah.fx.LFO;
import net.judah.fx.MonoFilter;
import net.judah.fx.Overdrive;
import net.judah.fx.Reverb;

public interface Pastels {

    Color RED = new Color(0xff6e8d);
    Color GREEN = new Color(0x90da6a);
    Color BLUE = new Color(189, 230, 250); //  98dafd // a4b9cb
    Color PINK = new Color(0xf4a2f9);
    Color PURPLE = new Color(0xa295ad); // 966FD6 //7955b5
    Color ORANGE = new Color(0xFFA500); // ffdf9e
    Color YELLOW = new Color(0xFFFF80);

    Color EGGSHELL = new Color(252,252,246);
	Color MY_GRAY = new Color(220, 220, 210);
    Color FADED = new Color(8, 8, 8, 25);
	Color BUTTONS = new Color(237, 237, 229);

	Color SHADE = BUTTONS;
	Color ONTAPE = GREEN;
	Color DOWNBEAT = alpha(BLUE, 90);
	Color GRID = MY_GRAY;
	Color SELECTED = ORANGE;
	Color CC = BLUE;
	Color PROGCHANGE = PINK;

	static Color alpha(Color input, int alpha) {
		return new Color(input.getRed(), input.getGreen(), input.getBlue(), alpha);
	}

	static Color getFx(Class<? extends Effect> class1) {
		if (Reverb.class.isAssignableFrom(class1))
			return RED;
		if (Overdrive.class.equals(class1))
			return YELLOW;
		if (Chorus.class.equals(class1))
			return GREEN;
		if (MonoFilter.class.equals(class1))
			return PINK;
		if (Filter.class.equals(class1))
			return PINK;
		if (EQ.class.equals(class1))
			return MY_GRAY;
		if (Delay.class.equals(class1))
			return ORANGE;
		if (Compressor.class.equals(class1))
			return PURPLE;
		if (LFO.class.equals(class1))
			return BLUE;
		if (Convolution.class.equals(class1))
			return Color.BLACK;
		return EGGSHELL;
	}
}

