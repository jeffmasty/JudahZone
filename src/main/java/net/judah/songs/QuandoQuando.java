package net.judah.songs;

import net.judah.MainFrame;
import net.judah.looper.Looper;
import net.judah.mixer.Channels;
import net.judah.tracker.Tracker;

public class QuandoQuando extends SmashHit {

	@Override
	public void startup(Tracker t, Looper loops, Channels ch) {
		super.startup(t, loops, ch);
		MainFrame.get().sheetMusic("QuandoQuando.png");
		
	}
	
}
