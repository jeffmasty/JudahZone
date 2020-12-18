package net.judah.mixer.plugin;

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
			Console.warn(e.getMessage(), e);
			return false;
		}
	}

	public void activate(boolean active) {
		try {
			getCarla().setActive(pluginIndex, active ? 1 : 0);
		} catch (Exception e) {
			Console.warn(e.getMessage(), e);
		}
	}
	
	private Carla getCarla() {
		return Sequencer.getCarla();
	}
}
//public void mute(boolean tOrF) {
//try {
//	if (tOrF)  previous = 
//} catch (Exception e) {
//	Console.warn(e.getMessage());
//}
//}

