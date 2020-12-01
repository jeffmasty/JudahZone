package net.judah.mixer.widget;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.judah.plugin.Carla;
import net.judah.sequencer.Sequencer;
import net.judah.util.Console;

@RequiredArgsConstructor 
public class CarlaVolume extends VolumeWidget {

	@Getter	private final int pluginIndex;
	
	@Override
	public boolean setVolume(float gain) {
		try {
			getCarla().setVolume(pluginIndex, gain * 1.27f); // Carla internal volume 0 to 1.27
			return true;
		} catch (Exception e) {
			Console.warn(e.getMessage());
			return false;
		}
	}

	public boolean mute(int tOrF) {
		try {
			getCarla().setActive(pluginIndex, tOrF);
			return true;
		} catch (Exception e) {
			Console.warn(e.getMessage());
			return false;
		}
	}
	
	private Carla getCarla() {
		return Sequencer.getCarla();
	}
}
