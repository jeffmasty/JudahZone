package net.judah.mixer.widget;

import net.judah.JudahZone;
import net.judah.fluid.FluidSynth;

public class FluidVolume extends VolumeWidget {

	float previous;
	
	@Override
	public boolean setVolume(float gain) {
		previous = gain;
		getFluid().gain(gain);
		return true;
	}

	private FluidSynth getFluid() {
		return (FluidSynth)JudahZone.getServices().byClass(FluidSynth.class);
	}
	
}
