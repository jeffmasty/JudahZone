package net.judah.midi;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import net.judah.effects.Fader;
import net.judah.effects.LFO;
import net.judah.util.RTLogger;

/** Checks up on any running LFOs and queues midi notes when the appropriate audio frame comes to pass*/
@Data @EqualsAndHashCode(callSuper=false)
public class MidiScheduler implements Runnable {

	@Getter private long current = -1;
	private final JudahMidi queue;
	private final BlockingQueue<Long> offering;

	public MidiScheduler(JudahMidi queue) {
		this.queue = queue;
		offering = new LinkedBlockingQueue<>(2);
	}

	@Override
	public void run() {
		try {
			while (true) {
				current = offering.take();
				LFO.pulse(); // query any running LFOs
				Fader.pulse();
			}
		} catch (Throwable t) {
			RTLogger.warn(this, t);
		}
	}

	public void offer(long currentTransportFrame) {
		offering.offer(currentTransportFrame);
	}

}
