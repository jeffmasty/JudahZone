package net.judah.mixer.plugin;

import net.judah.fluid.FluidSynth;

public class FluidVolume extends VolumeWidget {

	float previous;
	
	@Override
	public boolean setVolume(float gain) {
		previous = gain;
		FluidSynth.getInstance().gain(gain);
		return true;
	}

}
