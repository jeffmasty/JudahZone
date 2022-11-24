package net.judah.effects.gui;

import java.util.ArrayList;

import net.judah.JudahZone;
import net.judah.mixer.Channel;

public class PresetsHandler implements Widget {

	private final Channel ch;
	
	public PresetsHandler(Channel ch) {
		this.ch = ch;
	}

	@Override
	public void increment(boolean up) {
		int next = getIdx() + (up ? 1 : -1);
		if (next >= JudahZone.getPresets().size())
			next = 0;
		if (next < 0)
			next = JudahZone.getPresets().size() - 1;
		ch.setPreset(JudahZone.getPresets().get(next));
	}

	@Override
	public int getIdx() {
		return JudahZone.getPresets().indexOf(ch.getPreset());
	}

	@Override
	public ArrayList<String> getList() {
		return JudahZone.getPresets().getList();
	}
	
}
