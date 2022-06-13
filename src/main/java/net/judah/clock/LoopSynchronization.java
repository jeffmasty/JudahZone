package net.judah.clock;

import lombok.RequiredArgsConstructor;
import net.judah.api.Notification;
import net.judah.api.TimeListener;
import net.judah.looper.Loop;

@RequiredArgsConstructor
public class LoopSynchronization implements TimeListener {

	public static enum SelectType {
		ERASE, SYNC
	};

	
	private final Loop loop;
	private final int bars;
	private int counter = -1;
	private final JudahClock clock = JudahClock.getInstance();
	
	@Override public void update(Notification.Property prop, Object value) {
		
		if (Notification.Property.BARS != prop) return;
		if (counter == -1) {
			loop.record(true);
		}
		// TODO update SyncWidget
		counter ++;
		if (counter == bars) {
			loop.record(false);
			new Thread(() -> {
				clock.removeListener(this);
				clock.latch(loop);
			}).start();
		}
	}

}
