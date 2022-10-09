package net.judah.songs;

import net.judah.tracker.Track;

public class QuandoQuando extends SmashHit {

	// https://www.youtube.com/watch?v=JXEz7QZFn4c
	
	private int count;
	
	@Override
	public void startup() {
		super.startup();
		frame.sheetMusic("QuandoQuando.png");
		bass.setFile("QuandoBass");
		bass.getCycle().setCustom(this);
	}

	@Override
	public void cycle(Track t) {
		count++;
		if (count > 0) { 
			if (count % 8 == 0) {
				if (count == 32) {
					t.setPattern("Bridge1");
					count = -16;
				}
				else
					t.setCurrent(t.get(0));
			}
			else 
				t.next(true);
		} 
		else 
			t.next(true);
	
	}
	
}
