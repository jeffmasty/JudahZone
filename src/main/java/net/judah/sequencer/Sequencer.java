package net.judah.sequencer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.sound.midi.ControllerEventListener;
import javax.sound.midi.ShortMessage;

import org.apache.commons.lang3.StringUtils;
import org.jaudiolibs.jnajack.JackException;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j;
import net.judah.CommandHandler;
import net.judah.MainFrame;
import net.judah.Page;
import net.judah.fluid.FluidSynth;
import net.judah.midi.MidiClient;
import net.judah.midi.MidiPair;
import net.judah.midi.Route;
import net.judah.mixer.Mixer;
import net.judah.mixer.MixerCommands;
import net.judah.plugin.Carla;
import net.judah.plugin.Drumkv1;
import net.judah.settings.ActiveCommand;
import net.judah.settings.Command;
import net.judah.settings.CommandPair;
import net.judah.settings.Service;
import net.judah.settings.Services;
import net.judah.song.Song;
import net.judah.song.Trigger;
import net.judah.util.Console;
import net.judah.util.Constants;
import net.judah.util.JsonUtil;
import net.judah.util.JudahException;

@Log4j
public class Sequencer implements Service, Runnable, ControllerEventListener {
	
	public static enum ControlMode {
		/** internal clock */ 
		INTERNAL, 
		/** external clock (from looper, cc3 inserted into click tracks) */ 
		EXTERNAL};
	
	public static final String PARAM_LOOP = "Loop";
	public static final String PARAM_UNIT = "beats.per.pulse";
	public static final String PARAM_BPM = "bpm";
	public static final String PARAM_MEAUSRE = "bpb";
	public static final String PARAM_FLUID = "fluid";
	public static final String PARAM_CARLA = "Carla";
	/** CC3 messages inserted in midi clicktrack file define bars, see {@link #controlChange(ShortMessage)} */
	public static final String PARAM_CONTROLLED = "pulse.controlled";
	public static final String PARAM_SEQ_INTERNAL = "sequencer.internal";
	private static final String CONTROL_ERROR = "External control error: ";
	
	
	private final Command trigger = new Command("Trigger", this, "Move Sequencer to the next song section");
	private final Command end = new Command("Stop Sequencer", this, "stop song");
	private final Command externalControl = new Command("Time control", this, externalParams(), 
			"Move time clock from midi sequencer to a looper");
	private final Command dropBeatCmd = new Command("DropDaBeat", this, "Mute loops until next pulse");
	private final Command queuePlay = new ActiveCommand("Queue Play", this, MixerCommands.loopProps(), "play/stop on next pulse.");
	private final Command queueRecord = new ActiveCommand("Queue Record", this, MixerCommands.loopProps(), "record/stop on next pulse.");
	
	private ArrayList<Float> mixerState; 
	private Stack<CommandPair> queue = new Stack<CommandPair>();
	
	// TODO move clicktrack command here
	
	@Getter private final List<Command> commands = Arrays.asList(new Command[] 
			{trigger, end, externalControl, dropBeatCmd, queuePlay, queueRecord});
	@Getter private final String serviceName = Sequencer.class.getSimpleName();
	// @Getter private final Page page;
	@Getter private final Page page; 
	
	@Getter private float tempo = 80f;
	@Setter @Getter private int measure = 4;
	@Getter private final Song song;
	@Getter private File songfile;
	@Getter private final Services services = new Services();
	@Getter private final CommandHandler commander = new CommandHandler(this); 
	@Getter private final Carla carla;
	@Getter private final Metronome metronome;
	@Getter private final Mixer mixer;
	
	/** external (click track) uses cc3 messages and looper to set time */
	@Getter ControlMode control = ControlMode.INTERNAL;

	
	private Trigger active;
	private int index = 0;
	
	private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	private ScheduledFuture<?> callback;
	private int count = -1;
	/** number of beats per pulse */
	int pulse = 4;
	
	public Sequencer(File songfile) throws IOException, JackException {
		song = (Song)JsonUtil.readJson(songfile, Song.class);
		this.songfile = songfile;
		
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
		
		mixer = new Mixer(this);
		
		carla = initializeCarla();
		commander.initializeCommands();
		initializeTriggers();

		page = new Page(this);
		MainFrame.get().openPage(page);
		
	}
	
	private void initializeTriggers() {
		stop();
		if (song.getSequencer() == null)
			song.setSequencer(new ArrayList<>());
		List<Trigger> triggers = song.getSequencer();
		for (index = 0; index < triggers.size(); index++) {
			active = triggers.get(index);
			if (active.getTimestamp() >= 0) {
				return; // initialization done
			}
			
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
			control =  Boolean.parseBoolean(o.toString()) ? ControlMode.EXTERNAL : ControlMode.INTERNAL;
		
		if (StringUtils.isNumeric("" + props.get("bpm"))) {
			log.warn("props.get(bpm)  " + props.get("bpm"));
			tempo = Integer.parseInt("" + props.get("bpm"));
		}
		if (StringUtils.isNumeric("" + props.get("bpb")))
			setMeasure(Integer.parseInt("" + props.get("bpb")));
		
		if (props.containsKey("fluid")) {
			String[] split = props.get("fluid").toString().split(";");
			for (String cmd : split)
				FluidSynth.getInstance().sendCommand(cmd);
		}
		if (props.containsKey(Drumkv1.FILE_PARAM) && props.containsKey(Drumkv1.PORT_PARAM)) {
			MidiClient.getInstance().disconnectDrums();
			try {
				new Drumkv1(new File("" + props.get(Drumkv1.FILE_PARAM)), "" + props.get(Drumkv1.PORT_PARAM), false);
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
		if (props.containsKey(PARAM_UNIT)) {
			Object o2 = props.get(PARAM_UNIT);
			if (StringUtils.isNumeric("" + o2))
				pulse = Integer.parseInt(o2.toString());
		}
		
	}
	
	public boolean isRunning() {
		return count >= 0;
	}

	@Override public void close() {
		stop();
		services.remove(this);
		for (Service s : services)
			s.close();
	}

	public void reset() {
	}

	// dispose Song's services (metronome, loops, etc)
	public void stop() {
		if (!isRunning()) return;
		scheduler.shutdown();
		callback = null;
	}
	
	
	public void trigger() {
		if (active != null) {
			execute(active);
			increment();
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
	
	@Override
	public void execute(Command cmd, HashMap<String, Object> props) throws Exception {
		if (cmd == trigger) 
			trigger();
		if (cmd == end) 
			stop();
		if (cmd == externalControl) 
			externalControl(props);
		if (cmd == dropBeatCmd) {
			mixerState = mixer.muteAll();
			metronome.mute();
			queue.push(new CommandPair(dropBeatCmd, null));
		}
		if (cmd == queuePlay) 
			if (control == ControlMode.INTERNAL) {
				int candidate = count + 1;
				while (candidate % measure != 0) 
					candidate++;
				props.put(PARAM_SEQ_INTERNAL, candidate);
			}
			queue.push(new CommandPair(mixer.getCommands().getPlayCmd(), props));
		if (cmd == queueRecord)
			if (control == ControlMode.INTERNAL) {
				int candidate = count + 1;
				while (candidate % measure != 0) 
					candidate++;
				props.put(PARAM_SEQ_INTERNAL, candidate);
			}
			queue.push(new CommandPair(mixer.getCommands().getRecordCmd(), props));
	}

	private void execute(Trigger trig) {
		try {
			Command cmd = commander.find(trig.getService(), trig.getCommand());
			Console.info("@" + count + " seq execute: " + cmd + " " + Constants.prettyPrint(trig.getParams()));
			cmd.getService().execute(cmd, trig.getParams());
		} catch (Exception e) {
			log.error(e.getMessage() + " for " + trig, e);
		}
	}

	private void externalControl(HashMap<String, Object> props) {
		Object o = props.get(PARAM_LOOP);

		if (!StringUtils.isNumeric("" + o)) {
			log.error(CONTROL_ERROR + o);
			return;
		}
		int loop = Integer.parseInt(o.toString());
		if (mixer.getSamples().size() <= loop) {
			log.error(CONTROL_ERROR + " loop " + loop + " doesn't exist.");
			return;
		}

		Object o2 = props.get(PARAM_UNIT);
		if (o2 != null && StringUtils.isNumeric(o2.toString())) 
			pulse = Integer.parseInt(o2.toString());
		mixer.getSamples().get(loop).setTimeSync(true);
		log.warn("Looper " + loop + " has time control with pulse of " + pulse + " beats.");
		
	}

	private HashMap<String, Class<?>> externalParams() {
		HashMap<String, Class<?>> result = new HashMap<>(2);
		result.put(PARAM_LOOP, Integer.class);
		result.put(PARAM_UNIT, Integer.class);
		return result;
	}

	public void rollTransport() {
		if (isRunning()) return;
		
		count = 0;
		Console.debug(active.getTimestamp() + " " + active.getNotes()+ " " + Constants.prettyPrint(active.getParams()));
		if (active != null && active.getTimestamp() < 0)
			increment();
		
		while (active.getTimestamp() == count) { 
			execute(active);
			increment();
		}

		if (control == ControlMode.EXTERNAL) {
			Console.info("Rolling external, pulse: " + pulse);
			// will receive timing from CC3 messages in midi stream (ControlEventListener) or looper repeats (pulse) 
		}
		else {
			// start internal time
			long cycle = Constants.millisPerBeat(tempo);
			log.warn("Sequencer starting with a cycle of " + cycle + " for bpm: " + measure);
			Console.addText("Sequencer starting with a cycle of " + cycle + " for bpm: " + measure);
			
			callback = scheduler.scheduleAtFixedRate(this, Constants.millisPerBeat(tempo), 
					Constants.millisPerBeat(tempo), TimeUnit.MILLISECONDS);
			scheduler.schedule(
	    		new Runnable() {@Override public void run() {callback.cancel(true);}},
	    		24, TimeUnit.HOURS);
		}
	}

	/** external clock */
	public void pulse() {
		count += pulse;
		Console.addText("-- beat: " + count + " vs. " + active.getTimestamp());
		while (active.getTimestamp() == count) {
			execute(active);
			increment();
		}
		
		
		while (queue.isEmpty() == false) {
			CommandPair c = queue.pop();
			if (c.getCommand() == dropBeatCmd) { 
				Console.warn("UN-QUEUEU!!");
				try {
					metronome.unMute();
					mixer.restoreState(mixerState);
				} catch (JudahException e) {
					log.error(e.getMessage(), e);
					Console.warn("restore state: " + e.getMessage());
				}
			}
			else
				commander.fire(c.getCommand(), c.getProps());
		}
	}
	
	/** external clock */
	@Override
	public void controlChange(ShortMessage event) {
		if (event.getData1() != 3) 
			return;
		if (isRunning()) 
			pulse();
	}

	@Override public void run() {
		// if (count == -1) { /* initialization */ }
		++count;
		Console.addText("internal: " + count);
		while (active.getTimestamp() == count) {
			execute(active);
			increment();
		}
		
		if (queue.isEmpty()) return;
		
		CommandPair p = queue.peek();
		Object o = p.getProps().get(PARAM_SEQ_INTERNAL);
		if (o instanceof Integer && count == (Integer)o) {
			p = queue.pop();
			try {
				execute(p.getCommand(), p.getProps());
			} catch (Exception e) {
				log.error(e.getMessage(), e);
				Console.warn(e.getMessage());
			}
		}
		
	}

	public void setTempo(float tempo2) {
		if (tempo2 < tempo || tempo2 > tempo) {
			tempo = tempo2;
			metronome.setTempo(tempo);
		}
	}

}
