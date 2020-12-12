package net.judah.sequencer;
import static net.judah.util.Constants.Param.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

import javax.sound.midi.ShortMessage;

import org.apache.commons.lang3.StringUtils;
import org.jaudiolibs.jnajack.JackException;
import org.jaudiolibs.jnajack.JackTransportState;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j;
import net.judah.CommandHandler;
import net.judah.JudahZone;
import net.judah.MainFrame;
import net.judah.Page;
import net.judah.api.Command;
import net.judah.api.Loadable;
import net.judah.api.Service;
import net.judah.api.TimeListener;
import net.judah.looper.Recorder;
import net.judah.looper.Recording;
import net.judah.metronome.HiHats;
import net.judah.metronome.Metronome;
import net.judah.midi.JudahMidi;
import net.judah.midi.Route;
import net.judah.midi.Router;
import net.judah.mixer.Mixer;
import net.judah.plugin.Carla;
import net.judah.song.Song;
import net.judah.song.Trigger;
import net.judah.util.CommandPair;
import net.judah.util.Console;
import net.judah.util.Constants;
import net.judah.util.JsonUtil;
import net.judah.util.Services;

@Log4j
public class Sequencer implements Service, Runnable, TimeListener /* , ControllerEventListener */ {
	
	public static final String PARAM_PULSE = "beats.per.pulse";
	public static final String PARAM_FLUID = "fluid";
	public static final String PARAM_CARLA = "carla";
	public static final String PARAM_PATCH = "patch";
	/** CC3 messages inserted in midi clicktrack file define bars, see {@link #controlChange(ShortMessage)} */
	public static final String PARAM_CONTROLLED = "pulse.controlled";
	public static final String PARAM_SEQ_INTERNAL = "sequencer.internal";
	private static final String CONTROL_ERROR = "External control error: ";
	
	public static enum TimeBase {
		BEATS, PULSE, TICKS 
	}
	
	public static enum ControlMode {
		/** internal clock */ 
		INTERNAL, 
		/** external clock (from looper, cc3 inserted into click tracks) */ 
		EXTERNAL};
	
	@Getter private static Sequencer current; 
	
	@Getter private final String serviceName = Sequencer.class.getSimpleName();
	@Getter private final Song song;
	@Getter private File songfile;
	@Getter private final Services services = new Services();
	@Getter private final CommandHandler commander = new CommandHandler(this); 
	@Getter private static Carla carla;
	@Getter private final Mixer mixer;
	@Getter private final Metronome metronome;
	@Getter private final Page page; 
	@Getter private SeqCommands commands = new SeqCommands(this);

	/** external = looper (or deprecated cc3 events) sets the time */
	@Setter(value = AccessLevel.PACKAGE) @Getter 
	private ControlMode control = ControlMode.INTERNAL;
	/** internal clock */
	@Getter private SeqClock clock;
	
	final Stack<CommandPair> queue = new Stack<CommandPair>();
	
	/** the next command sitting in the sequencer awaiting execution. */
	@Getter private Trigger active;
	/** current sequencer command index */
	private int index = 0;
	/** a sense of the current beat */
	@Getter private int count = -1;
	
	@Getter private float tempo = 80f;
	
	@Setter(value = AccessLevel.PACKAGE) @Getter 
	private int measure = 4;
	/** number of TimeBase units (beats) per pulse */
	@Setter(value = AccessLevel.PACKAGE) @Getter 
	private int pulse = 4;
	/** msec since transport start or msec between pulses */
	private long timer = 0;
	// 	private TimeBase unit = TimeBase.BEATS;
	// for dropDaBeat, currently not implemented
	@SuppressWarnings("unused")
	private ArrayList<Float> mixerState; 
	
	public Sequencer(File songfile) throws IOException, JackException {
		this.songfile = songfile;
		song = (Song)JsonUtil.readJson(songfile, Song.class);
		services.add(this);
		metronome = JudahZone.getMetronome();
		commander.clearMappings();
		commander.addMappings(song.getLinks());
		
		current = handlePrevious();
		mixer = new Mixer(this);
		commander.initializeCommands();
		initializeProperties();
		initializeTriggers();
		initializeFiles();
		Router midi = JudahMidi.getInstance().getRouter();
		song.getRouter().forEach( pair -> { midi.add(
				new Route(pair.getFromMidi(), pair.getToMidi()));});
		page = new Page(this);
		MainFrame.get().openPage(page);
	}

	private Sequencer handlePrevious() {
		JudahMidi.getInstance().getRouter().clear();
		Sequencer previous = Sequencer.getCurrent();
		if (previous != null) {
			JudahZone.getMetronome().removeListener(previous);
			previous.close();
		}
		metronome.addListener(this);
		log.debug("loaded song: " + songfile.getAbsolutePath());
		return this;
	}
	
	private void initializeProperties() {
		final HashMap<String, Object> props = song.getProps();
		if (props == null) return;
		
		JudahZone.getServices().forEach(service -> { service.properties(props);});
		getServices().forEach(service -> {service.properties(props);});
	}

	private void initializeTriggers() {
		List<Trigger> triggers = song.getSequencer();
		if (triggers.size() == 0) return;
		active = triggers.get(0);
		while (active.go(count)) 
			execute(active);
//		for (index = 0; index < triggers.size(); index++) {
//			active = triggers.get(index);
//			if ( false == active.getType().equals(Type.INIT)) 
//				return; // initialization done
//			
//			Command cmd = active.getCmd();
//			if (cmd == null) {
//				Constants.infoBox("Failed to initialize: " + cmd, "Sequencer Initialization");
//				log.error("Failed to initialize: " + cmd);
//				continue;
//			}
//			execute(active);
//		}
//		if (active == null) {
//			HashMap<String, Object> params = new HashMap<>();
//			params.put(ACTIVE, false);
//			active = new Trigger(Type.REL, 0l, commands.transport.getName(), "ze End.", params, commands.transport);
//		}
	}
	
	private void initializeFiles() {
		song.getSequencer().forEach(trigger -> {
			if(trigger.getCmd() instanceof Loadable)
				try {
					((Loadable)trigger.getCmd()).load(trigger.getParams());
				} catch (Exception e) { 
					Console.warn(e.getMessage());
				}});
			
		song.getLinks().forEach(link -> {
			if(link.getCmd() instanceof Loadable) 
				try {
					((Loadable)link.getCmd()).load(link.getProps());
				} catch (Exception e) { 
					Console.warn(e.getMessage());
				}});

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
		song.getSequencer().forEach(trigger -> {
			if (trigger.getCmd() instanceof Loadable)
					((Loadable)trigger.getCmd()).close();});
			
		song.getLinks().forEach(link -> {
			if (link.getCmd() instanceof Loadable) 
				((Loadable)link.getCmd()).close();});
		
	}

	// dispose Song's services (loops, etc)
	public void stop() {
		// if (!isRunning()) return;
		if (clock != null) {
			clock.stop();
		}
		HiHats.stop();
		mixer.stopAll();
		count = 0;
	}
	
//	/** external clock */
//	@Override // ControlChange Listener deprecate? see TimeListener    
//	public void controlChange(ShortMessage event) {
//		if (event.getData1() != 3) 
//			return;
//		if (isRunning()) 
//			pulse();
//	}
//
	@Override public void run() { // Thread
		// if (count == -1) { /* initialization */ }
		++count;
		Console.addText("internal: " + count);
		while (active.go(count)) 
			execute(active);
		
		if (queue.isEmpty()) return;
		
		CommandPair p = queue.peek();
		Object o = p.getProps().get(PARAM_SEQ_INTERNAL);
		if (o instanceof Integer && count == (Integer)o) {
			p = queue.pop();
			try {
				p.getCommand().setSeq(this);
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
		
		if (Property.TRANSPORT == prop) {
			if (value == JackTransportState.JackTransportStarting) {
				if (control == ControlMode.INTERNAL) {
					if (clock == null) clock = new SeqClock(this);
					log.warn("Internal Sequencer starting with a bpm of " + getTempo());
					Console.addText("Internal Sequencer starting with a bpm of " + getTempo());
					JudahZone.getMetronome().removeListener(this);
					timer = System.currentTimeMillis();
					clock.addListener(this);
					clock.start();
					
				}
			}
			else if (value == JackTransportState.JackTransportStopped)
				stop();
		}
		if (Property.BEAT == prop) { 
			// if (!isRunning()) return;
			if (control == ControlMode.INTERNAL) {
				log.trace("beat update: " + value);
				count = (int)value;
				while (active.go(count)) 
					execute(active);

				checkQueue();
			}
		}
		if (Property.LOOP == prop) {
			long period = (System.currentTimeMillis() - timer) / pulse;
			HiHats.setPeriod(period);
			if (control == ControlMode.EXTERNAL) {
				count++;
				log.debug("loop update: " + count);
				while (active.go(count)) 
					execute(active);
				checkQueue();
				
				if (clock != null)
					clock.pulse(timer);
				timer = System.currentTimeMillis();
			}
		}
	}
	
	private void checkQueue() {
		if (queue.isEmpty()) return;

		CommandPair c = queue.peek();
		while(c != null) {
			try {

				int i = Integer.parseInt("" + c.getProps().get(PARAM_SEQ_INTERNAL));
				if (i > count) return;
				queue.pop();
				c.getCommand().setSeq(this);
				c.getCommand().execute(c.getProps(), -1);
			} catch (Throwable t) {
				Console.warn("queue error for " + c + " " + t.getMessage());
				return;
			}
		}
// TODO handle dropDaBeat		
//		if (c.getCommand() == commands.dropBeat) { 
//			Console.warn("UN-QUEUEU!!");
//			try { // TODO metronome.unMute();
//				mixer.restoreState(mixerState);
//			} catch (JudahException e) {
//				log.error(e.getMessage(), e);
//				Console.warn("restore state: " + e.getMessage());
//			}}
//		else commander.fire(c.getCommand(), c.getProps(), -1);}
	}
	
	@Override // Service
	public void properties(HashMap<String, Object> props) {
		
		// Initialize Carla
		if (props.containsKey(PARAM_CARLA)) 
			carla = initializeCarla("" + props.get(PARAM_CARLA), true);
		
		if (carla != null)
			carla.getCommands().forEach( c -> {commander.addCommand(c);});
		
		Object o = props.get(PARAM_CONTROLLED);
		if (o != null) 
			control =  Boolean.parseBoolean(o.toString()) ? ControlMode.EXTERNAL : ControlMode.INTERNAL;
		
		if (StringUtils.isNumeric("" + props.get(BPM))) {
			log.warn("props.get(bpm)  " + props.get(BPM));
			tempo = Integer.parseInt("" + props.get(BPM));
		}
		if (StringUtils.isNumeric("" + props.get(MEASURE)))
			measure = Integer.parseInt("" + props.get(MEASURE));
		
		if (props.containsKey(PARAM_PULSE)) {
			Object o2 = props.get(PARAM_PULSE);
			if (StringUtils.isNumeric("" + o2))
				pulse = Integer.parseInt(o2.toString());
		}
	}
	
/////////////////////////////////////////////////////////////////////////////////////////////

	void trigger() {
		if (active != null) {
			execute(active);
			while (active.go(count)) {
				execute(active);
			}
		}
		
	}

	void externalControl(HashMap<String, Object> props) {
		Object o = props.get(LOOP);

		if (!StringUtils.isNumeric("" + o)) {
			log.error(CONTROL_ERROR + o);
			return;
		}
		int loop = Integer.parseInt(o.toString());
		if (mixer.getSamples().size() <= loop) {
			log.error(CONTROL_ERROR + " loop " + loop + " doesn't exist.");
			return;
		}

		Object o2 = props.get(PARAM_PULSE);
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

	void queue(CommandPair command) {
		if (command != null)
			queue.push(command);
	}
	
	void internal(String name, String param) {
		if ("AndILoveHer".equals(name)) 
			_andILoveHer();
		if ("FeelGoodInc".equals(name))
			_feelGoodInc();
		// if ("bassdrum".equals(name)) _bassdrum(param);
	}

/////////////////////////////////////////////////////////////////////////////////////////////
	

	private void execute(Trigger trig) {
		try {
			Command cmd = trig.getCmd();
			Console.info("seq @" + count + "/" + index + " execute: " + cmd + " " + Constants.prettyPrint(trig.getParams()));
			cmd.setSeq(this);
			cmd.execute(trig.getParams(), -1);
		} catch (Exception e) {
			log.error(e.getMessage() + " for " + trig, e);
		}
		increment();
	}

	private void increment() {
		index++;
		if (index < song.getSequencer().size())
			active = song.getSequencer().get(index);
		else {
			log.warn("We've reached the end of the sequencer");
			active = new Trigger(-2l, commands.transport);
			if (clock != null)
				clock.stop();
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

	/**Play sample 0 and it becomes time master. 
	 * sample 1 is 5 times longer and is empty */
	private void _andILoveHer() {
		Recorder drums = (Recorder)mixer.getSamples().get(0);
		Recording sample = drums.getRecording();
		mixer.getSamples().get(1).setRecording(new Recording(sample.size() * 5, true));
		control = ControlMode.EXTERNAL;
		clock.stop();
		// if (clock != null) clock.removeListener(this);
		pulse = 8;
		timer = System.currentTimeMillis();
		Metronome.remove(this);
		drums.addListener(this);
		log.info("internal: _allMyLoving()");
	}

	private void _feelGoodInc() {
		Recorder drums = (Recorder)mixer.getSamples().get(0);
		Recorder track2 = (Recorder)mixer.getSamples().get(1);
		
		Recording sample = drums.getRecording();
		pulse = 16;
		timer = System.currentTimeMillis();
		count = 0;
		
		track2.setRecording(new Recording(sample.size() * 2, true));

		control = ControlMode.EXTERNAL;
		clock.stop();
		log.info("internal: _feelGoodInc()");
	}

	public void setClock(SeqClock clock) {
		if (this.clock != null)
			this.clock.stop();
		this.clock = clock;
	}
}


//private void _bassdrum(String param) {
//try {
//	if (ControlMode.INTERNAL != control)
//		throw new JudahException("bass drums on internal clock only");
//	int clicks = Integer.parseInt(param);
//	// clock.doBassDrum(clicks); TODO put in sequencer
//
//} catch (Throwable t) {
//	Console.warn("bassdrum " + t.getMessage() + " param=" + param);
//}
//}
//public void rollTransport() {
//if (isRunning()) return;
//count = 0;
//// Console.debug(active.getTimestamp() + " " + active.getNotes()+ " " + Constants.prettyPrint(active.getParams()));
//if (active != null && active.getTimestamp() < 0)
//	increment();
//while (active.go(count)) {
//	execute(active);
//	increment();
//}
//if (control == ControlMode.EXTERNAL) {
//	Console.info("Rolling external, pulse: " + pulse);
//	// will receive timing from CC3 messages in midi stream (ControlEventListener) or looper repeats (pulse) 
//}
//else {
//	// start internal time
//	internal = new Internal(this);
//	log.warn("Internal Sequencer starting with a bpm of " + getTempo());
//		Console.addText("Internal Sequencer starting with a bpm of " + getTempo());
//	internal.start();
////	callback = scheduler.scheduleAtFixedRate(this, Constants.millisPerBeat(tempo), 
////			Constants.millisPerBeat(tempo), TimeUnit.MILLISECONDS);
////	scheduler.schedule(
////		new Runnable() {@Override public void run() {callback.cancel(true);}},
////		24, TimeUnit.HOURS);
//}
//}
///** external clock */
//void pulse() {
//	count += pulse;
//	Console.addText("-- beat: " + count + " vs. " + active.getTimestamp());
//	while (active.go(count)) {
//		execute(active);
//		increment();
//	}
//	
//	
//	while (queue.isEmpty() == false) {
//		CommandPair c = queue.pop();
//		if (c.getCommand() == commands.dropBeat) { 
//			Console.warn("UN-QUEUEU!!");
//			try {
//				
//				// TODO
//				// metronome.unMute();
//				
//				mixer.restoreState(mixerState);
//			} catch (JudahException e) {
//				log.error(e.getMessage(), e);
//				Console.warn("restore state: " + e.getMessage());
//			}
//		}
//		else
//			commander.fire(c.getCommand(), c.getProps(), -1);
//	}
//}
