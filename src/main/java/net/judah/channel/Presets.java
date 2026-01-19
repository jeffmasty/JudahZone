package net.judah.channel;

public interface Presets {

	Preset getPreset();
	void setPreset(Preset p);
	Preset toPreset(String name);
	String getName();
}
