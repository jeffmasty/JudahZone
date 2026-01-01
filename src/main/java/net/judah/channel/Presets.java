package net.judah.channel;

import net.judah.mixer.Preset;

public interface Presets {

	Preset getPreset();
	void setPreset(Preset p);
	Preset toPreset(String name);
	String getName();
}
