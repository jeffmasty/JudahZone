package net.judah.songs;

import net.judah.MainFrame;
import net.judah.api.Notification.Property;
import net.judah.api.Status;
import net.judah.api.TimeListener;
import net.judah.looper.Looper;
import net.judah.mixer.Channels;
import net.judah.tracker.JudahBeatz;

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
	public void startup(JudahBeatz t, Looper loops, Channels ch, MainFrame frame) {
		super.startup(t, loops, ch, frame);
		// TODO drums tracks and tempo
		
		
		frame.sheetMusic("Four.png");
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
