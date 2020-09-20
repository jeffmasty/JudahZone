package net.judah.metronome;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j;
import net.judah.CommandHandler;
import net.judah.mixer.Mixer;
import net.judah.settings.Command;
import net.judah.settings.Service;
import net.judah.settings.Services;
import net.judah.song.Trigger;
import net.judah.util.Constants;
import net.judah.util.Tab;

@Log4j
public class Sequencer implements Service, Runnable {
	@Getter private static Sequencer instance = new Sequencer();
	
	private final Command trigger = new Command("Trigger", this, "Move Sequencer to the next song section");
	private final Command end = new Command("Stop Sequencer", this, "stop song");
	@Getter private final List<Command> commands = Arrays.asList(new Command[] {trigger, end});
	@Getter private final String serviceName = Sequencer.class.getSimpleName();
	@Getter private final Tab gui = null;
	
	@Setter @Getter private float tempo = 80;
	@Setter @Getter private int measure = 4;

	private List<Trigger> triggers;
	private Trigger active;
	private int index = 0;
	
	private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	private ScheduledFuture<?> callback;
	private int count = -1;
	
	private Sequencer() {
		instance = this;
		Services.getInstance().add(this);
	}
	
	public void initialize(List<Trigger> triggers) {
		stop();
		this.triggers = triggers;

		for (index = 0; index < triggers.size(); index++) {
			active = triggers.get(index);
			if (active.getTimestamp() >= 0) return; // initialization done
			
			Command cmd = CommandHandler.find(active.getService(), active.getCommand());
			if (cmd == null) {
				Constants.infoBox("Failed to initialize: " + cmd, "Sequencer Initialization");
				log.error("Failed to initialize: " + cmd);
				continue;
			}
			execute(active);
		}
		if (active == null)
			active = new Trigger(-2, end);
	}

	
	public boolean isRunning() {
		return callback != null;
	}

	@Override public void close() {
	}

	public void reset() {
	}

	public void stop() {
		if (!isRunning()) return;
		scheduler.shutdown();
		callback = null;
		
		Mixer.getInstance().stopAll();
		
	}
	
	
	public void trigger() {
		if (active != null) {
			execute(active);
			increment();
		}
	}

	public void rollTransport() {
		if (isRunning()) return;

		// start time
		long cycle = Constants.millisPerBeat(tempo);
		log.warn("Sequencer starting with a cycle of " + cycle + " for bpm: " + measure);

		callback = scheduler.scheduleAtFixedRate(this, 0, 
				Constants.millisPerBeat(tempo), TimeUnit.MILLISECONDS);
		scheduler.schedule(
    		new Runnable() {@Override public void run() {callback.cancel(true);}},
    		24, TimeUnit.HOURS);
	}

	private void increment() {
		index++;
		if (index < triggers.size())
			active = triggers.get(index);
		else {
			log.warn("We've reached the end of the sequencer");
			active = new Trigger(-2l, end);
		}
	}
	
	@Override public void run() {
		// if (count == -1) { /* initialization */ }

		count++;
		while (active.getTimestamp() == count) {
			execute(active);
			increment();
		}
	}

	@Override
	public void execute(Command cmd, HashMap<String, Object> props) throws Exception {
		if (cmd == trigger) 
			trigger();
		if (cmd == end) 
			stop();
	}

	private void execute(Trigger trig) {
		try {
			Command cmd = CommandHandler.find(trig.getService(), trig.getCommand());
			log.warn("@" + count + " seq execute: " + cmd + " " + Constants.prettyPrint(trig.getParams()));
			cmd.getService().execute(cmd, trig.getParams());
		} catch (Exception e) {
			log.error(e.getMessage() + " for " + trig, e);
		}
	}

}
