package net.judah.gui.scope;

import net.judah.util.Recording;

public interface Live {

	public static record LiveData(Live processor, Recording stereo) {}

	void analyze(Recording rec);

}
