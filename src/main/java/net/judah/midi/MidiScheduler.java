package net.judah.midi;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import judahzone.api.FX;
import judahzone.util.RTLogger;
import lombok.Getter;
import net.judah.JudahZone;
import net.judah.channel.Channel;
import net.judah.gui.MainFrame;
import net.judah.mixer.Fader;

/** increments any running LFOs */
public class MidiScheduler implements Runnable {

	private final JudahZone zone;

	public MidiScheduler(JudahZone zone) {
		this.zone = zone;
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
		if (zone.getMains().isOnMute())
			return;
		for (Channel ch : zone.getMixer().getAll()) {

			FX fx1 = ch.isActive(ch.getLfo()) ? ch.getLfo().pulse() : null;
			FX fx2 = ch.isActive(ch.getLfo2()) ? ch.getLfo2().pulse() : null;
			if (fx1 != null)
				MainFrame.updateFx(ch, fx1);
			if (fx2 != null)
				MainFrame.updateFx(ch, fx1);
		}
	}

}
