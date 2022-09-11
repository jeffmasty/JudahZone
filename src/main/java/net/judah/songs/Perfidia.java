package net.judah.songs;

import net.judah.MainFrame;
import net.judah.looper.Looper;
import net.judah.settings.Channels;
import net.judah.tracker.Tracker;

public class Perfidia extends SmashHit {

	@Override
	public void startup(Tracker t, Looper loops, Channels ch) {
		super.startup(t, loops, ch);
		MainFrame.get().sheetMusic("Perfidia.png");
		t.getClock().setLength(8);
		t.getBass().setFile("Perfidia");
	}
	
}
