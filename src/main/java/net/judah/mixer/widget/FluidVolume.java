package net.judah.mixer.widget;

import net.judah.fluid.FluidSynth;
import net.judah.settings.Services;

public class FluidVolume extends VolumeWidget {

	float previous;
	
	@Override
	public boolean setVolume(float gain) {
		previous = gain;
		getFluid().gain(gain);
		return true;
	}

	private FluidSynth getFluid() {
		return (FluidSynth)Services.byClass(FluidSynth.class);
	}
	
}
