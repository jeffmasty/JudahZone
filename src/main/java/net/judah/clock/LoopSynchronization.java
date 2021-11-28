package net.judah.clock;

import lombok.RequiredArgsConstructor;
import net.judah.api.TimeListener;
import net.judah.looper.Recorder;

@RequiredArgsConstructor
public class LoopSynchronization implements TimeListener {

	public static enum SelectType {
		ERASE, SYNC
	};

	
	private final Recorder loop;
	private final int bars;
	private int counter = -1;
	private final JudahClock clock = JudahClock.getInstance();
	
	@Override public void update(Property prop, Object value) {
		
		if (Property.BARS != prop) return;
		if (counter == -1) {
			loop.record(true);
		}
			
		counter ++;
		if (counter == bars) {
			loop.record(false);
			new Thread(() -> {clock.removeListener(this);}).start();
		}
	}

}
