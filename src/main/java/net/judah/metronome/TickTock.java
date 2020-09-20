package net.judah.metronome;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.ShortMessage;

import lombok.ToString;
import lombok.extern.log4j.Log4j;
import net.judah.midi.Midi;
import net.judah.midi.MidiClient;
import net.judah.util.Constants;

/** sequence our own metronome */
@Log4j @ToString
public class TickTock implements MetroPlayer {
	
	public static final int DEFAULT_DOWNBEAT = 34;
	public static final int DEFAULT_BEAT = 33;
	MidiClient midi = MidiClient.getInstance();
	Sequencer sequencer = Sequencer.getInstance();
	
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	private ScheduledFuture<?> beeperHandle;
    private final WakeUp wakeUp = new WakeUp();
	private final AtomicBoolean changed = new AtomicBoolean(true);
    

	private Midi downbeatOn, downbeatOff, beatOn, beatOff; //:>
	private int gain = 99; // data2 midi gain
	
	private Integer duration;
	private Integer intro; 
	
	class WakeUp implements Runnable {

		private int count = 0;
		@Override public void run() {

			if (changed.compareAndSet(true, false)) { // initilization
				beeperHandle.cancel(true);
		        beeperHandle = scheduler.scheduleAtFixedRate(
		        		this, 0, Constants.millisPerBeat(sequencer.getTempo()), TimeUnit.MILLISECONDS);
		        scheduler.schedule(
		        		new Runnable() {@Override public void run() {beeperHandle.cancel(true);}},
		        		24, TimeUnit.HOURS);
		        return;
			}
			
			// count beats then trigger timebase or stop ticktock
			if (intro != null && count == intro) {
				sequencer.rollTransport();
			}
			if (duration != null && count >= duration) {
				stop();
				return;
			}
			
			boolean first = count % sequencer.getMeasure() == 0;
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
	TickTock(int downbeat, int beat, int channel) {
    	try {
    		downbeatOn = new Midi(ShortMessage.NOTE_ON, channel, downbeat, gain); 
    		downbeatOff = new Midi(ShortMessage.NOTE_OFF, channel, downbeat);
    		beatOn = new Midi(ShortMessage.NOTE_ON, channel, beat, gain); 
    		beatOff = new Midi(ShortMessage.NOTE_OFF, channel, beat);
    	} catch (InvalidMidiDataException e) {
    		log.error(e.getMessage(), e);
    	}
	}

	/** bell and woodblock on channel 9 */
	TickTock() {
		this(34, 33, 9); 
	}

	@Override
	public boolean isRunning() {
		return beeperHandle != null;
	}

	@Override
	public void start() {
		if (isRunning()) return;

		long cycle = Constants.millisPerBeat(sequencer.getTempo());
		log.debug("Metronome starting with a cycle of " + cycle + " for bpm: " + Sequencer.getInstance().getMeasure());

		beeperHandle = scheduler.scheduleAtFixedRate(wakeUp, 0, 
				Constants.millisPerBeat(sequencer.getTempo()), TimeUnit.MILLISECONDS);
		scheduler.schedule(
    		new Runnable() {@Override public void run() {beeperHandle.cancel(true);}},
    		24, TimeUnit.HOURS);
	}

	@Override
	public void stop() {
		if (!isRunning()) return;
		scheduler.shutdown();
		beeperHandle = null;
	}

	@Override
	public void setGain(float gain) {
		int midiVolume = Constants.gain2midi(gain);
		try {
			downbeatOn = new Midi(downbeatOn.getCommand(), downbeatOn.getChannel(), downbeatOn.getData1(), midiVolume);
			beatOn = new Midi(beatOn.getCommand(), beatOn.getChannel(), beatOn.getData1(), midiVolume);
		} catch (InvalidMidiDataException e) {
			log.error(e.getMessage(),e);
		}
	}

	@Override
	public void close() {
		stop();
	}
	
	@Override
	public void setDuration(Integer intro, Integer duration) {
		this.intro = intro;
		this.duration = duration;
	}
	
}
