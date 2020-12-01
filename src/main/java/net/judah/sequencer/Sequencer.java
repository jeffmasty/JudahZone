package net.judah.sequencer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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
import net.judah.JudahZone;
import net.judah.MainFrame;
import net.judah.Page;
import net.judah.api.TimeListener;
import net.judah.looper.Recorder;
import net.judah.looper.Recording;
import net.judah.midi.JudahMidi;
import net.judah.midi.MidiPair;
import net.judah.midi.Route;
import net.judah.mixer.Mixer;
import net.judah.plugin.Carla;
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
public class Sequencer implements Service, Runnable, TimeListener, ControllerEventListener {
	
	public static final String PARAM_LOOP = "loop";
	public static final String PARAM_UNIT = "beats.per.pulse";
	public static final String PARAM_FLUID = "fluid";
	public static final String PARAM_CARLA = "carla";
	public static final String PARAM_PATCH = "patch";
	/** CC3 messages inserted in midi clicktrack file define bars, see {@link #controlChange(ShortMessage)} */
	public static final String PARAM_CONTROLLED = "pulse.controlled";
	public static final String PARAM_SEQ_INTERNAL = "sequencer.internal";
	private static final String CONTROL_ERROR = "External control error: ";
	
	public static enum ControlMode {
		/** internal clock */ 
		INTERNAL, 
		/** external clock (from looper, cc3 inserted into click tracks) */ 
		EXTERNAL};
	
	@Getter SeqCommands commands = new SeqCommands(this);
	
	private ArrayList<Float> mixerState; 
	private Stack<CommandPair> queue = new Stack<CommandPair>();
	
	@Getter private final String serviceName = Sequencer.class.getSimpleName();
	@Getter private final Page page; 
	
	@Getter private float tempo = 80f;
	@Setter @Getter private int measure = 4;
	@Getter private final Song song;
	@Getter private File songfile;
	@Getter private final Services services = new Services();
	@Getter private final CommandHandler commander = new CommandHandler(this); 
	@Getter private static Carla carla;
	@Getter private final Mixer mixer;
	
	/** external (click track) uses cc3 messages and looper to set time */
	@Getter ControlMode control = ControlMode.INTERNAL;

	private Trigger active;
	
	private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	private ScheduledFuture<?> callback;
	/** number of beats per pulse */
	int pulse = 4;
	/** a sense of the current beat */
	private int count = -1;
	/** current sequencer command index */
	private int index = 0;
	
	public Sequencer(File songfile) throws IOException, JackException {
		song = (Song)JsonUtil.readJson(songfile, Song.class);
		this.songfile = songfile;
		
		services.add(this);
		commander.clearMappings();
		commander.addMappings(song.getLinks());
		
		JudahMidi midi = JudahMidi.getInstance();
		midi.getRouter().clear();
		
		if (song.getRouter() != null) {
			for (MidiPair pair : song.getRouter()) 
				midi.getRouter().add(new Route(pair.getFromMidi(), pair.getToMidi()));
			log.debug("midi router handling " + midi.getRouter().size() + " translations");
		}		
		
		mixer = new Mixer(this);
		
		commander.initializeCommands();
		
		initializeProperties();
		initializeTriggers();

		page = new Page(this);
		MainFrame.get().openPage(page);
		
	}

	private void initializeProperties() {
		final HashMap<String, Object> props = song.getProps();
		if (props == null) return;
		
		JudahZone.getServices().forEach(service -> { service.properties(props);});
		getServices().forEach(service -> {service.properties(props);});
	}

	private void initializeTriggers() {
		if (song.getSequencer() == null)
			song.setSequencer(new ArrayList<>());
		List<Trigger> triggers = song.getSequencer();
		for (index = 0; index < triggers.size(); index++) {
			active = triggers.get(index);
			if (active.getTimestamp() >= 0) {
				return; // initialization done
			}
			
			Command cmd = commander.find(active.getCommand());
			if (cmd == null) {
				Constants.infoBox("Failed to initialize: " + cmd, "Sequencer Initialization");
				log.error("Failed to initialize: " + cmd);
				continue;
			}
			execute(active);
		}
		if (active == null)
			active = new Trigger(-2, commands.end);
	}
///////////////////////////////////////////////////////////////////////////////////////////	
	
	public boolean isRunning() {
		return count >= 0;
	}

	@Override public void close() {
		stop();
		services.remove(this);
		for (Service s : services)
			s.close();
	}

	// dispose Song's services (loops, etc)
	public void stop() {
		if (!isRunning()) return;
		scheduler.shutdown();
		callback = null;
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
	@Override // ControlChange Listener deprecate? see TimeListener    
	public void controlChange(ShortMessage event) {
		if (event.getData1() != 3) 
			return;
		if (isRunning()) 
			pulse();
	}

	@Override public void run() { // Thread
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
				p.getCommand().execute(p.getProps(), -1);
			} catch (Exception e) {
				log.error(e.getMessage(), e);
				Console.warn(e.getMessage());
			}
		}
		
	}

	public void setTempo(float tempo2) {
		if (tempo2 < tempo || tempo2 > tempo) {
			tempo = tempo2;
			if (JudahZone.getMetronome() != null)
				JudahZone.getMetronome().setTempo(tempo2);
		}
	}

	@Override // TimeListener
	public void update(Property prop, Object value) {
		if (Property.BEAT == prop) {
			count = (int)value;
			while (active.getTimestamp() == count) {
				execute(active);
				increment();
			}
		}
		if (Property.LOOP == prop) {
			count++;
			while (active.getTimestamp() == count) {
				execute(active);
				increment();
			}
		}
	}
	
	@Override // Service
	public void properties(HashMap<String, Object> props) {
		
		// Initialize Carla
		if (props.containsKey(PARAM_CARLA))
			carla = initializeCarla("" + props.get(PARAM_CARLA), true);
		
		Object o = props.get(PARAM_CONTROLLED);
		if (o != null) 
			control =  Boolean.parseBoolean(o.toString()) ? ControlMode.EXTERNAL : ControlMode.INTERNAL;
		
		if (StringUtils.isNumeric("" + props.get("bpm"))) {
			log.warn("props.get(bpm)  " + props.get("bpm"));
			tempo = Integer.parseInt("" + props.get("bpm"));
		}
		if (StringUtils.isNumeric("" + props.get("bpb")))
			setMeasure(Integer.parseInt("" + props.get("bpb")));
		
		if (props.containsKey(PARAM_UNIT)) {
			Object o2 = props.get(PARAM_UNIT);
			if (StringUtils.isNumeric("" + o2))
				pulse = Integer.parseInt(o2.toString());
		}
	}
	
/////////////////////////////////////////////////////////////////////////////////////////////
	
	void trigger() {
		if (active != null) {
			execute(active);
			increment();
		}
	}
	
	/** external clock */
	void pulse() {
		count += pulse;
		Console.addText("-- beat: " + count + " vs. " + active.getTimestamp());
		while (active.getTimestamp() == count) {
			execute(active);
			increment();
		}
		
		
		while (queue.isEmpty() == false) {
			CommandPair c = queue.pop();
			if (c.getCommand() == commands.dropBeat) { 
				Console.warn("UN-QUEUEU!!");
				try {
					
					// TODO
					// metronome.unMute();
					
					mixer.restoreState(mixerState);
				} catch (JudahException e) {
					log.error(e.getMessage(), e);
					Console.warn("restore state: " + e.getMessage());
				}
			}
			else
				commander.fire(c.getCommand(), c.getProps(), -1);
		}
	}

	void externalControl(HashMap<String, Object> props) {
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

	void dropDaBeat(CommandPair cmds) {
		mixerState = mixer.muteAll();
		JudahZone.getMetronome().setVolume(0);
		queue.push(cmds);
	}

	void qPlay(HashMap<String, Object> props) {
		if (control == ControlMode.INTERNAL) {
			int candidate = count + 1;
			while (candidate % measure != 0) 
				candidate++;
			props.put(PARAM_SEQ_INTERNAL, candidate);
		}
		queue.push(new CommandPair(mixer.getCommands().getPlayCmd(), props));
	}
	
	void qRecord(HashMap<String, Object> props) {
		if (control == ControlMode.INTERNAL) {
			int candidate = count + 1;
			while (candidate % measure != 0) 
				candidate++;
			props.put(PARAM_SEQ_INTERNAL, candidate);
		}
		queue.push(new CommandPair(mixer.getCommands().getRecordCmd(), props));

	}

	void internal(String name) {
		if ("AllMyLoving".equals(name)) 
			_allMyLoving();
	}

/////////////////////////////////////////////////////////////////////////////////////////////
	
	private void increment() {
		index++;
		if (index < song.getSequencer().size())
			active = song.getSequencer().get(index);
		else {
			log.warn("We've reached the end of the sequencer");
			active = new Trigger(-2l, commands.end);
		}
	}

	private void execute(Trigger trig) {
		try {
			Command cmd = commander.find(trig.getCommand());
			Console.info("@" + count + " seq execute: " + cmd + " " + Constants.prettyPrint(trig.getParams()));
			cmd.execute(trig.getParams(), -1);
		} catch (Exception e) {
			log.error(e.getMessage() + " for " + trig, e);
		}
	}

	private Carla initializeCarla(String carlaSettings, boolean showGui) {
		try {
			if (carla != null) {
				if (carla.getSettings().equals(carlaSettings))
					return carla;
				carla.close();
			}
			return new Carla(carlaSettings, showGui);
		} catch (Throwable t) {
			log.error(carlaSettings + ": " + t.getMessage(), t);
			Constants.infoBox(t.getMessage() + " for " + carlaSettings, "Song Error");
			return null;
		}
	}

	private void _allMyLoving() {
		Recorder drums = (Recorder)mixer.getSamples().get(0);
		Recording sample = drums.getRecording();
	
		assert sample != null : sample;
		assert mixer.getSamples().get(1) != null : mixer.getSamples().size();
		
		mixer.getSamples().get(1).setRecording(new Recording(sample.size() * 5, true));
		drums.play(true);
		drums.addListener(Sequencer.this);
		log.info("internal: _allMyLoving()");
	}

}
