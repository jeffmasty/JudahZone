package net.judah.midi;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import net.judah.api.Midi;
import net.judah.api.MidiQueue;
import net.judah.effects.Fader;
import net.judah.effects.LFO;
import net.judah.sequencer.MidiEvent;
import net.judah.sequencer.MidiTrack;
import net.judah.util.Constants;
import net.judah.util.RTLogger;

/** Checks up on any running LFOs and queues midi notes when the appropriate audio frame comes to pass*/
@Data @EqualsAndHashCode(callSuper=false)
public class MidiScheduler extends MidiTrack implements Runnable {

	@Getter private long current = -1;
	private final MidiQueue queue;
	private final BlockingQueue<Long> offering;

	public MidiScheduler(MidiQueue queue) {
		this.queue = queue;
		offering = new LinkedBlockingQueue<>(2);
	}

	@Override
	public void run() {
		MidiEvent schedule;
		MidiTrack track;
		ScheduledEvent event;
		Midi msg;
		try {
			while (true) {
				current = offering.take();
				LFO.pulse(); // query any running LFOs
				Fader.pulse();

				if (isEmpty()) continue;
				schedule = get(0);
				if (schedule.getOffset() < current - 2) {
					RTLogger.warn(this, "skipping " + schedule.getMsg() + " " + schedule.getOffset() + " vs. " + current);
					remove(0);
				}
				else if (schedule.getOffset() <= current) {
					event = (ScheduledEvent)remove(0);
					track = event.getOwner();
					if (!track.isActive()) continue;
					msg = Constants.transpose(event.getMsg(),
							track.getTranspose(), track.getGain());
					if (track.getOutput() == null)
						queue.queue(msg);
					else {
						track.getOutput().queue(msg);
					}
				}
			}
		} catch (Throwable t) {
			RTLogger.warn(this, t);
		}
	}

	public void addTrack(Long reference, MidiTrack track) {
		for (MidiEvent e : track) {
			long time = reference + e.getOffset();
			assert time > 0 : "reference: " + reference + " offset: " + e.getOffset();
			if (time >= current && time < current + 4)
				queue.queue(e.getMsg());
			else if (time > current) {
				add(new ScheduledEvent(time, e.getMsg(), track));
			}
			// else skip
		}
	}

	public void offer(long currentTransportFrame) {
		offering.offer(currentTransportFrame);
	}

}
