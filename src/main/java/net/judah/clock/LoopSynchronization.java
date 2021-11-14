package net.judah.clock;

import lombok.RequiredArgsConstructor;
import net.judah.api.TimeListener;
import net.judah.looper.Recorder;

@RequiredArgsConstructor
public class LoopSynchronization implements TimeListener {

	private final JudahClock clock;
	private final Recorder loop;
	
	@Override public void update(Property prop, Object value) {
		
	}

}
