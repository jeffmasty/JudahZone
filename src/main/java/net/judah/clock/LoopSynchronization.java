package net.judah.clock;

import lombok.RequiredArgsConstructor;
import net.judah.api.Notification;
import net.judah.api.TimeListener;
import net.judah.looper.Recorder;
import net.judah.util.Constants;
import net.judah.util.RTLogger;

@RequiredArgsConstructor
public class LoopSynchronization implements TimeListener {

	public static enum SelectType {
		ERASE, SYNC
	};

	
	private final Recorder loop;
	private final int bars;
	private int counter = -1;
	private final JudahClock clock = JudahClock.getInstance();
	
	@Override public void update(Notification.Property prop, Object value) {
		
		if (Notification.Property.BARS != prop) return;
		if (counter == -1) {
			loop.record(true);
		}
			
		counter ++;
		if (counter == bars) {
			loop.record(false);
			new Thread(() -> {
				clock.removeListener(this);
				float tempo = Constants.computeTempo(loop.getRecordedLength(), JudahClock.getLength() * clock.getMeasure());
				RTLogger.log(this, "[" + loop.getName() + "] " + loop.getRecordedLength()/1000f + "s. " + 
							+ clock.getTempo() + " vs " + tempo + " bpm " + clock.getInterval() + " interval.");
				//clock.setTempo(tempo);
				clock.latch(loop);
			}).start();
		}
	}

}
