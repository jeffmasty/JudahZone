package net.judah.gui.fx;

import java.util.ArrayList;

import net.judah.JudahZone;
import net.judah.mixer.Channel;

public class PresetsHandler {

	private final Channel ch;
	
	public PresetsHandler(Channel ch) {
		this.ch = ch;
	}

	public void increment(boolean up) {
		int next = getIdx() + (up ? 1 : -1);
		if (next >= JudahZone.getPresets().size())
			next = 0;
		if (next < 0)
			next = JudahZone.getPresets().size() - 1;
		ch.setPreset(JudahZone.getPresets().get(next));
	}

	public int getIdx() {
		return JudahZone.getPresets().indexOf(ch.getPreset());
	}

	public ArrayList<String> getList() {
		return JudahZone.getPresets().getList();
	}
	
}
