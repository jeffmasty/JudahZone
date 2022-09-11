package net.judah.songs;

import net.judah.MainFrame;
import net.judah.api.Notification.Property;
import net.judah.api.Status;
import net.judah.api.TimeListener;
import net.judah.looper.Looper;
import net.judah.settings.Channels;
import net.judah.tracker.Tracker;

public class Four extends SmashHit {

	TimeListener muteA = new TimeListener() {
		@Override public void update(Property prop, Object value) {
			if (value == Status.TERMINATED) {
				loops.getLoopB().removeListener(this);
				loops.getLoopA().setOnMute(true); // pick up melody and solo after punching in chords
			}
		}
	};
	
	@Override
	public void startup(Tracker t, Looper loops, Channels ch) {
		super.startup(t, loops, ch);
		// TODO drums tracks and tempo
		
		
		MainFrame.get().sheetMusic("Four.png");
		t.getClock().setLength(8); // in double time
		loops.getLoopB().setArmed(true);
		loops.getLoopB().addListener(muteA);
		loops.getLoopA().setOnMute(false);
		loops.getLoopB().setOnMute(false);
	}
	
	@Override
	public void teardown() {
		loops.getLoopB().removeListener(muteA);
	}
	
}
