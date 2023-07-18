package net.judah.fx;

import java.awt.Color;

import net.judah.gui.Pastels;

public class EffectColor implements Pastels {

	public static Color get(Class<? extends Effect> class1) {
		if (Reverb.class.isAssignableFrom(class1)) 
			return RED;
		if (Overdrive.class.equals(class1))
			return YELLOW;
		if (Chorus.class.equals(class1))
			return GREEN;
		if (Filter.class.equals(class1))
			return PINK;
		if (EQ.class.equals(class1)) 
			return Color.LIGHT_GRAY;
		if (Delay.class.equals(class1)) 
			return ORANGE;
		if (Compressor.class.equals(class1))
			return PURPLE;
		if (LFO.class.equals(class1))
			return BLUE;
		return EGGSHELL;
	}
	
}
