package net.judah.fluid;

import java.util.ArrayList;

class FluidChannels extends ArrayList<FluidChannel> {
	public static final int CHANNELS = 4;
	

	/** @return preset instrument index for the channel */
	public String getCurrentPreset(int channel) {
		for (FluidChannel c : this)
			if (c.channel == channel)
				return c.name;
		return "none";
	}
	public int getBank(int channel) {
		for (FluidChannel c : this)
			if (c.channel == channel)
				return c.bank;
		return -1;
	}
	
}
