package net.judah.metronome;

import static net.judah.settings.Commands.MetronomeLbls.*;
import static net.judah.util.Constants.*;

import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.ShortMessage;

import lombok.extern.log4j.Log4j;
import net.judah.api.Command;
import net.judah.api.Midi;
import net.judah.api.MidiClient;
import net.judah.api.TimeListener;
import net.judah.sequencer.Sequencer;
import net.judah.util.Constants;

@Log4j
public class HiHats extends Command implements TimeListener {

	private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	private static final int DOWN = 42; // down hi hat midi
	private static final int UP = 46; // up hi hat midi 
	private static Midi downbeat, upbeat, downoff, upoff;
	private static final AtomicBoolean changed = new AtomicBoolean(true);
	private static ScheduledFuture<?> beeperHandle;
	private static long period; 
    
	public HiHats(Metronome metro, float gain) throws InvalidMidiDataException {
		super(HIHATS.name, HIHATS.desc, Command.activeTemplate());
		
		downbeat = new Midi(ShortMessage.NOTE_ON, 9, DOWN, gain2midi(gain));
		upbeat = new Midi(ShortMessage.NOTE_ON, 9, UP, gain2midi(gain));
		downoff = new Midi(ShortMessage.NOTE_OFF, 9, DOWN);
		upoff = new Midi(ShortMessage.NOTE_OFF, 9, UP);
		metro.addListener(this);
	}

	static class WakeUp implements Runnable {

		private final MidiClient midi = Metronome.getMidi();

	    private boolean even = true;

		@Override public void run() {

			if (changed.compareAndSet(true, false)) { // Initialization or tempo update
				if (beeperHandle != null)
					beeperHandle.cancel(true);
				if (period == 0)
					period = Constants.millisPerBeat(Sequencer.getCurrent().getTempo() * 2);
		        beeperHandle = scheduler.scheduleAtFixedRate(
		        		this, period, period, TimeUnit.MILLISECONDS);
		        scheduler.schedule(
		        		new Runnable() {@Override public void run() {beeperHandle.cancel(true);}},
		        		24, TimeUnit.HOURS);
			}
			midi.queue(even ? downbeat : upbeat);
	        even = !even;
		}
	}
	
	@Override
	public void execute(HashMap<String, Object> props, int midiData2) throws Exception {
		boolean active = false;
		if (midiData2 > 0) 
			active = true;
		else if (props.containsKey(Command.PARAM_ACTIVE)) 
			try { active = Boolean.parseBoolean("" + props.get(Command.PARAM_ACTIVE)); }
				catch (Throwable t) { log.debug(t.getMessage());}
		if (active) {
			changed.set(true);
			new WakeUp().run();
		}
		else 
			stop();
	}

	public static void stop() {
		if (beeperHandle == null) return;
		beeperHandle.cancel(true);
		beeperHandle = null;
		Metronome.getMidi().queue(downoff);
		Metronome.getMidi().queue(upoff);
	}

	public static void setPeriod(long millisPerBeat) {
		period = millisPerBeat / 2;
		if (beeperHandle != null) {
			beeperHandle.cancel(true);
			changed.set(true);
			new WakeUp().run();
		}
	}

	@Override
	public void update(Property prop, Object value) {
		if (Property.VOLUME == prop) {
			int volume = gain2midi((float)value);
			if (volume != downbeat.getData2()) 
				try {
					downbeat = new Midi(ShortMessage.NOTE_ON, 9, DOWN, volume);
					upbeat = new Midi(ShortMessage.NOTE_ON, 9, UP, volume);
				} catch (Throwable t) { log.debug(t.getMessage(), t);}
		}
	}
	
}
