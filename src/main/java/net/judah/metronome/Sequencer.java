package net.judah.metronome;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.sound.midi.ControllerEventListener;
import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.ShortMessage;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j;
import net.judah.CommandHandler;
import net.judah.JudahZone;
import net.judah.fluid.FluidSynth;
import net.judah.midi.MidiClient;
import net.judah.midi.MidiPair;
import net.judah.midi.Route;
import net.judah.mixer.Mixer;
import net.judah.plugin.Carla;
import net.judah.plugin.Drumkv1;
import net.judah.settings.Command;
import net.judah.settings.Service;
import net.judah.settings.Services;
import net.judah.song.Song;
import net.judah.song.SongTab;
import net.judah.song.Trigger;
import net.judah.util.Constants;
import net.judah.util.Tab;

@Log4j
public class Sequencer implements Service, Runnable, MetaEventListener, ControllerEventListener {
	
	public static final String PARAM_BPM = "bpm";
	public static final String PARAM_MEAUSRE = "bpb";
	public static final String PARAM_FLUID = "fluid";
	public static final String PARAM_CARLA = "Carla";
	/** CC3 messages inserted in midi clicktrack file define bars, see {@link #controlChange(ShortMessage)} */
	public static final String PARAM_CONTROLLED = "midi.controlled";
	
	private final Command trigger = new Command("Trigger", this, "Move Sequencer to the next song section");
	private final Command end = new Command("Stop Sequencer", this, "stop song");
	// TODO clicktrack
	
	@Getter private final List<Command> commands = Arrays.asList(new Command[] {trigger, end});
	@Getter private final String serviceName = Sequencer.class.getSimpleName();
	@Getter private final Tab gui = null;
	
	@Getter private float tempo = 80;
	@Setter @Getter private int measure = 4;
	@Getter private final Song song;
	@Getter private File songfile;
	@Getter private final Services services = new Services();
	@Getter private final CommandHandler commander = new CommandHandler(this); 
	@Getter final private Carla carla;
	@Getter final private Metronome metronome;
	
	boolean midiControlled = false; // click track uses cc3 messages to set tempo
	private int cc3 = -1;
	private Trigger active;
	private int index = 0;
	
	private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	private ScheduledFuture<?> callback;
	private int count = -1;
	
	public Sequencer(Song song, File songfile) {
		this.song = song;
		this.songfile = songfile;

		if (JudahZone.getCurrentSong() != null) {
			JudahZone.getCurrentSong().stop();
		}
		
		services.add(this);
		commander.clearMappings();
		commander.addMappings(song.getLinks());
		
		MidiClient midi = MidiClient.getInstance();
		midi.getRouter().clear();
		
		if (song.getRouter() != null) {
			for (MidiPair pair : song.getRouter()) 
				midi.getRouter().add(new Route(pair.getFromMidi(), pair.getToMidi()));
			log.debug("midi router handling " + midi.getRouter().size() + " translations");
		}		
		
		
		initializeProperties();
		metronome = new Metronome(this);
		Mixer.getInstance().setMetronome(metronome);
		carla = initializeCarla();
		commander.initializeCommands();
		initializeTriggers();
		
		JudahZone.openTab(new SongTab(this));
		
	}
	
	private void initializeTriggers() {
		stop();
		if (song.getSequencer() == null)
			song.setSequencer(new ArrayList<>());
		List<Trigger> triggers = song.getSequencer();
		for (index = 0; index < triggers.size(); index++) {
			active = triggers.get(index);
			if (active.getTimestamp() >= 0) return; // initialization done
			
			Command cmd = commander.find(active.getService(), active.getCommand());
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
	
	private Carla initializeCarla() {
		HashMap<String, Object> props = song.getProps();
		if (!props.containsKey("Carla")) return null;
		try {
			File file = new File("" + props.get("Carla"));
			Carla carla = Carla.getInstance();
			if (carla == null)
				new Carla(file);
			else 
				carla.reload(file); 
			return carla;
		} catch (Throwable t) {
			log.error(props.get("Carla") + ": " + t.getMessage(), t);
			Constants.infoBox(t.getMessage() + " for " + props.get("Carla"), "Song Error");
			return null;
		}
		
	}
	
	private void initializeProperties() {
		final HashMap<String, Object> props = song.getProps();
		if (props == null) return;
		
		Object o = props.get(PARAM_CONTROLLED);
		if (o != null) 
			midiControlled = Boolean.parseBoolean(o.toString());
		
		if (StringUtils.isNumeric("" + props.get("bpm"))) {
			log.warn("props.get(bpm)  " + props.get("bpm"));
			tempo = Integer.parseInt("" + props.get("bpm"));
		}
		if (StringUtils.isNumeric("" + props.get("bpb")))
			setMeasure(Integer.parseInt("" + props.get("bpb")));
		
		if (props.containsKey("fluid")) {
			String[] split = props.get("fluid").toString().split(";");
			for (String cmd : split)
				((FluidSynth)services.byClass(FluidSynth.class)).sendCommand(cmd);
		}
		if (props.containsKey(Drumkv1.FILE_PARAM) && props.containsKey(Drumkv1.PORT_PARAM)) {
			MidiClient.getInstance().disconnectDrums();
			try {
				new Drumkv1(new File("" + props.get(Drumkv1.FILE_PARAM)), "" + props.get(Drumkv1.PORT_PARAM), false);
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
		
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
		if (metronome.isRunning()) 
			metronome.close();
	}
	
	
	public void trigger() {
		if (active != null) {
			execute(active);
			increment();
		}
	}

	public void rollTransport() {
		if (isRunning()) return;


		if (midiControlled) {
			// pick up timing from CC3 messages in midi stream (ControlEventListener)
		}
		else {
			// start internal time
			long cycle = Constants.millisPerBeat(tempo);
			log.warn("Sequencer starting with a cycle of " + cycle + " for bpm: " + measure);

			callback = scheduler.scheduleAtFixedRate(this, 0, 
					Constants.millisPerBeat(tempo), TimeUnit.MILLISECONDS);
			scheduler.schedule(
	    		new Runnable() {@Override public void run() {callback.cancel(true);}},
	    		24, TimeUnit.HOURS);
		}
	}

	private void increment() {
		index++;
		if (index < song.getSequencer().size())
			active = song.getSequencer().get(index);
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
			Command cmd = commander.find(trig.getService(), trig.getCommand());
			log.warn("@" + count + " seq execute: " + cmd + " " + Constants.prettyPrint(trig.getParams()));
			cmd.getService().execute(cmd, trig.getParams());
		} catch (Exception e) {
			log.error(e.getMessage() + " for " + trig, e);
		}
	}

	@Override
	public void meta(MetaMessage meta) {
		// log.warn("meta rcv'd: " + meta.getStatus() + "." + meta.getType());
	}

	@Override
	public void controlChange(ShortMessage event) {
		if (event.getData1() != 3) return;
		count = ++cc3 * measure;
		log.warn("-- beat: " + count + " vs. " + active.getTimestamp());
		while (active.getTimestamp() == count) {
			execute(active);
			increment();
		}
	}
	

}
