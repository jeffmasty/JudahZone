package net.judah.songs;

import net.judah.looper.Looper;
import net.judah.mixer.Channels;
import net.judah.tracker.Tracker;

public class AllBlues extends SmashHit {

	@Override
	public void startup(Tracker t, Looper loops, Channels ch) {
		t.getClock().writeTempo(80);
		t.getClock().setLength(12);
		
		super.startup(t, loops, ch);
	}
	
}
