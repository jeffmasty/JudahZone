package net.judah.songs;

import net.judah.api.Notification.Property;
import net.judah.api.Status;
import net.judah.api.TimeListener;

public class Four extends SmashHit {

	TimeListener muteA = new TimeListener() {
		@Override public void update(Property prop, Object value) {
			if (value == Status.TERMINATED) {
				looper.getLoopB().removeListener(this);
				looper.getLoopA().setOnMute(true); // pick up melody and solo after punching in chords
			}
		}
	};
	
	@Override
	public void startup() {
		super.startup();
		// TODO drums tracks and tempo
		
		
		frame.sheetMusic("Four.png");
		clock.setLength(8); // in double time
		looper.getLoopB().setArmed(true);
		looper.getLoopB().addListener(muteA);
		looper.getLoopA().setOnMute(false);
		looper.getLoopB().setOnMute(false);
	}
	
	@Override
	public void teardown() {
		looper.getLoopB().removeListener(muteA);
	}
	
}
