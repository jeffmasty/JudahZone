package net.judah.songs;

import net.judah.MainFrame;
import net.judah.looper.Looper;
import net.judah.settings.Channels;
import net.judah.tracker.Tracker;

public class Estate extends SmashHit {

	@Override
	public void startup(Tracker t, Looper loops, Channels ch) {
		super.startup(t, loops, ch);
		t.getClock().setLength(7);
		MainFrame.get().sheetMusic("Estate.png");
		
	}
	
}
