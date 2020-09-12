package net.judah.mixer.widget;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.judah.plugin.Carla;
import net.judah.settings.Services;

@RequiredArgsConstructor 
public class CarlaVolume extends VolumeWidget {

	@Getter	private final int pluginIndex;
	
	@Override
	public boolean setVolume(float gain) {
		return getCarla().setVolume(pluginIndex, gain * 1.27f); // Carla internal volume 0 to 1.27	
	}

	public boolean mute(int tOrF) {
		return getCarla().setActive(pluginIndex, tOrF);
	}
	
	private Carla getCarla() {
		return (Carla)Services.byClass(Carla.class);
	}
}
