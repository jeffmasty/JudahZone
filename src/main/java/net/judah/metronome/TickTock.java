package net.judah.metronome;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.ShortMessage;

import lombok.extern.log4j.Log4j;
import net.judah.api.Midi;
import net.judah.api.MidiQueue;
import net.judah.api.Status;
import net.judah.util.Constants;


/** sequence our own metronome */
@Log4j
public class TickTock implements Player {

	public static final int DEFAULT_DOWNBEAT = 45;
	public static final int DEFAULT_BEAT = 37;
	private final MidiQueue midi;
	private final Metronome metronome;
    private int measure = 4;
    private float tempo = 85;

	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	private ScheduledFuture<?> beeperHandle;
    private final WakeUp wakeUp = new WakeUp();
	private final AtomicBoolean changed = new AtomicBoolean(true);

	@SuppressWarnings("unused")
	private Midi downbeatOn, downbeatOff, beatOn, beatOff; //:>

	private Integer duration;
	private Integer intro;

	class WakeUp implements Runnable {

		private int count = 0;
		@Override public void run() {

			if (changed.compareAndSet(true, false)) { // initilization or tempo update
				beeperHandle.cancel(true);
		        beeperHandle = scheduler.scheduleAtFixedRate(
		        		this, 0, Constants.millisPerBeat(tempo), TimeUnit.MILLISECONDS);
		        scheduler.schedule(
		        		new Runnable() {@Override public void run() {beeperHandle.cancel(true);}},
		        		24, TimeUnit.HOURS);
		        return;
			}

			// count beats then trigger timebase or stop ticktock
			if (intro != null && count == intro) {
				metronome.rollTransport(); // roll transport
			}
			if (duration != null && count >= duration) {

				log.info("duration hit " + count);

				// metronome.beat(duration - intro);
				beeperHandle.cancel(true);
				metronome.end();
				return;
			}

			boolean first = count % measure == 0;
			midi.queue(first ? downbeatOn : beatOn);
			// drum machines don't like note off, Fluid synth doesn't mind...
			// midi.queue(first ? downbeatOff : beatOff);
			count++;
		}
	}

	/**
	 * @param downbeat data1 of noteOn for downbeat
	 * @param beat data1 of noteOn for normal beat
	 * @param measure beats per measure
	 * @param tempo
	 */
	TickTock(Metronome metro, int downbeat, int beat, int channel) {
		this.midi = metro.getMidi();
		this.metronome = metro;
    	try {
    		downbeatOn = new Midi(ShortMessage.NOTE_ON, channel, downbeat, 100);
    		downbeatOff = new Midi(ShortMessage.NOTE_OFF, channel, downbeat);
    		beatOn = new Midi(ShortMessage.NOTE_ON, channel, beat, 100);
    		beatOff = new Midi(ShortMessage.NOTE_OFF, channel, beat);
    	} catch (InvalidMidiDataException e) {
    		log.error(e.getMessage(), e);
    	}
	}

	/** bell and woodblock on channel 9 */
	TickTock(Metronome metro) {
		this(metro, DEFAULT_DOWNBEAT, DEFAULT_BEAT, 9);
	}

	@Override
	public boolean isRunning() {
		return beeperHandle != null;
	}

	@Override
	public void start() {
		if (isRunning()) return;

		long cycle = Constants.millisPerBeat(tempo);
		log.debug("TickTock starting with a cycle of " + cycle + " for bpm: " + tempo);

		beeperHandle = scheduler.scheduleAtFixedRate(wakeUp, 0,
				Constants.millisPerBeat(tempo), TimeUnit.MILLISECONDS);
		scheduler.schedule(
    		new Runnable() {@Override public void run() {beeperHandle.cancel(true);}},
    		24, TimeUnit.HOURS);
	}

	@Override
	public void stop() {
		if (!isRunning()) return;
		beeperHandle.cancel(true);
		beeperHandle = null;
	}

	public void setGain(float gain) {
		int midiVolume = Constants.gain2midi(gain);
		try {
			downbeatOn = new Midi(downbeatOn.getCommand(), downbeatOn.getChannel(), downbeatOn.getData1(), midiVolume);
			beatOn = new Midi(beatOn.getCommand(), beatOn.getChannel(), beatOn.getData1(), midiVolume);
		} catch (InvalidMidiDataException e) {
			log.error(e.getMessage(),e);
		}
	}

	public void setTempo(float tempo) {
		this.tempo = tempo;
		changed.set(true);
	}

	@Override
	public void close() {
		stop();
	}

	// @Override from TimeProvider
	@Override
	public void setDuration(Integer intro, Integer duration) {
		this.intro = intro;
		this.duration = duration;
	}

	@Override
	public void update(Property prop, Object value) {
		if (value == Status.ACTIVE) start();
		if (value == Status.TERMINATED) stop();
		if (prop == Property.TEMPO) setTempo((Float)value);
		if (prop == Property.VOLUME) setGain((Float)value);
		if (prop == Property.MEASURE) measure = (Integer)value;
	}

}
