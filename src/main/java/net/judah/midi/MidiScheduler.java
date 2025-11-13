package net.judah.midi;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.fx.Fader;
import net.judah.mixer.Channel;
import net.judah.util.RTLogger;

/** increments any running LFOs */
public class MidiScheduler implements Runnable {

	public MidiScheduler() {
		new Thread(this).start();
	}

	@Getter private long current = -1;
	private final BlockingQueue<Long> offering = new LinkedBlockingQueue<>(2);
	private int count = 0;

	@Override public void run() {
		try {
			while (true) {
				current = offering.take();
				if (++count > 1) { // throttle
					pulseLFOs(); // query any running LFOs
					Fader.pulse();
					count = 0;
				}
			}
		} catch (Throwable t) {
			RTLogger.warn(this, t);
		}
	}

	public void offer(long currentTransportFrame) {
		offering.offer(currentTransportFrame);
	}

	void pulseLFOs() {
		if (JudahZone.getMains().isOnMute())
			return;
		for (Channel ch : JudahZone.getMixer().getAll()) {
			ch.getLfo().pulse();
			ch.getLfo2().pulse();
		}
	}

}
