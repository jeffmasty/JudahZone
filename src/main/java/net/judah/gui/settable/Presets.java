package net.judah.gui.settable;

import net.judah.fx.Preset;

public interface Presets {

	Preset getPreset();
	void setPreset(Preset p);
	Preset toPreset(String name);
	String getName();
}
