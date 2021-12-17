package net.judah.effects;

import java.awt.Color;

import net.judah.effects.api.Effect;
import net.judah.effects.api.Reverb;
import net.judah.util.Pastels;

public class EffectColor implements Pastels {

	public static Color get(Class<? extends Effect> class1) {
		if (Reverb.class.isAssignableFrom(class1)) 
			return RED;
		if (Overdrive.class.equals(class1))
			return YELLOW;
		if (Chorus.class.equals(class1))
			return GREEN;
		if (CutFilter.class.equals(class1))
			return PINK;
		if (EQ.class.equals(class1)) 
			return Color.LIGHT_GRAY;
		if (Delay.class.equals(class1)) 
			return ORANGE;
		if (Compression.class.equals(class1))
			return PURPLE;
		if (LFO.class.equals(class1))
			return BLUE;
		return MY_GRAY;
	}
	
}
