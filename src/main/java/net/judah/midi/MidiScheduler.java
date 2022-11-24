package net.judah.midi;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import net.judah.JudahZone;
import net.judah.drumz.DrumKit;
import net.judah.drumz.DrumSample;
import net.judah.effects.Fader;
import net.judah.mixer.Channel;
import net.judah.samples.Sample;
import net.judah.util.RTLogger;

/** Checks up on any running LFOs and queues midi notes when the appropriate audio frame comes to pass*/
@Data @EqualsAndHashCode(callSuper=false)
public class MidiScheduler implements Runnable {

	@Getter private long current = -1;
	private final BlockingQueue<Long> offering;
	
	public MidiScheduler() {
		offering = new LinkedBlockingQueue<>(2);
	}

	@Override
	public void run() {
		try {
			while (true) {
				current = offering.take();
				pulseLFOs(); // query any running LFOs
				Fader.pulse();
			}
		} catch (Throwable t) {
			RTLogger.warn(this, t);
		}
	}

	public void offer(long currentTransportFrame) {
		offering.offer(currentTransportFrame);
	}

	void pulseLFOs() {
		for (Channel ch : JudahZone.getMixer().getChannels()) 
			ch.getLfo().pulse();
		for (Sample s : JudahZone.getSampler())
			s.getLfo().pulse();
		for (DrumKit k : JudahZone.getDrumMachine().getDrumkits())
			for (DrumSample s : k.getSamples())
				s.getLfo().pulse();
	}

	
}
