package net.judah.songs;

import net.judah.MainFrame;
import net.judah.looper.Looper;
import net.judah.mixer.Channels;
import net.judah.tracker.JudahBeatz;

public class Estate extends SmashHit {

	@Override
	public void startup(JudahBeatz t, Looper loops, Channels ch, MainFrame frame) {
		super.startup(t, loops, ch, frame);
		t.getClock().setLength(7);
		frame.sheetMusic("Estate.png");
		
	}
	
}
