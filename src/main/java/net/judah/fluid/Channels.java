package net.judah.fluid;

import java.util.ArrayList;

@SuppressWarnings("serial")
public class Channels extends ArrayList<FluidChannel> {

	/** @return preset instrument index for the channel */
	public int getCurrentPreset(int channel) {
		for (FluidChannel c : this) {
			if (c.channel == channel) {
				return c.preset;
			}
		}
		return -1;
	}



}
