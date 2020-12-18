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
import net.judah.metronome.Metronome;
import net.judah.midi.JudahMidi;
import net.judah.midi.Route;
import net.judah.midi.Router;
import net.judah.mixer.Mixer;
import net.judah.plugin.Carla;
import net.judah.song.Song;
import net.judah.song.Trigger;
import net.judah.util.CommandWrapper;
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
	private SeqDisplay display;

	/** external = looper (or deprecated cc3 events) sets the time */
	@Setter(value = AccessLevel.PACKAGE) @Getter 
	private ControlMode control = ControlMode.INTERNAL;
	/** internal clock */
	@Getter private SeqClock clock;
	
	final Stack<CommandWrapper> queue = new Stack<CommandWrapper>();
	
	/** the next command sitting in the sequencer awaiting execution. */
	@Getter private Trigger active;
	/** current sequencer command index */
	private int index = 0;

	/** a sense of the current beat */
	@Getter private int count = -1;
	
	/** number of TimeBase units (beats) per pulse */
	@Setter(value = AccessLevel.PACKAGE) @Getter 
	private int pulse = 4;
	
	// 	private TimeBase unit = TimeBase.BEATS; 
	// for dropDaBeat, currently not implemented
	@SuppressWarnings("unused") private ArrayList<Float> mixerState; 
	
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
		initializeFiles();
		initializeTriggers();
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
	}
	
	private void initializeFiles() {
		song.getSequencer().forEach(trigger -> {
			if(trigger.getCmd() instanceof Loadable)
				try {
					((Loadable)trigger.getCmd()).load(trigger.getParams());
				} catch (Exception e) { 
					Console.warn(e.getMessage(), e);
				}});
			
		song.getLinks().forEach(link -> {
			if(link.getCmd() instanceof Loadable) 
				try {
					((Loadable)link.getCmd()).load(link.getProps());
				} catch (Exception e) { 
					Console.warn(e.getMessage(), e);
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
			clock.end();
		}
		mixer.stopAll();
		count = 0;
	}
	
	@Override public void run() { // Thread
		++count;
		while (active.go(count)) 
			execute(active);
		checkQueue();
	}

	void setClock(SeqClock clock) {
		if (this.clock != null)
			this.clock.end();
		this.clock = clock;
	}

	@Override // TimeListener
	public void update(Property prop, Object value) {
		
		if (Property.TRANSPORT == prop) {
			if (value == JackTransportState.JackTransportStarting) {
				if (control == ControlMode.INTERNAL) {
					if (clock == null) clock = new SeqClock(this);
					JudahZone.getMetronome().removeListener(this);
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
			//long period = (System.currentTimeMillis() - timer) / pulse;
			if (control == ControlMode.EXTERNAL) {
				count++;
				Console.info("loop update: " + count);
				while (active.go(count)) 
					execute(active);
				checkQueue();
				if (clock != null)
					clock.pulse();
			}
		}
	}
	
	private void checkQueue() {
		if (queue.isEmpty()) return;

		CommandWrapper c = queue.peek();
		while (c != null && c.getInternalCount() == count) {
			queue.pop();
			try {
				c.getCommand().setSeq(this);
				c.getCommand().execute(c.getProps(), -1);
			} catch (Throwable t) {
				Console.warn("queue error for " + c + " " + t.getMessage(), t);
				return;
			}
			c = queue.peek();
		}
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
		
		if (props.containsKey(PARAM_PULSE)) {
			Object o2 = props.get(PARAM_PULSE);
			if (StringUtils.isNumeric("" + o2))
				pulse = Integer.parseInt(o2.toString());
		}
	}
	
	public SeqDisplay getGui() {
		if (display == null) {
			display = new SeqDisplay();
			if (clock != null) 
				clock.addListener(display);
			else 
				log.debug("no clock");
		}
		return display;
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

	void dropDaBeat(CommandWrapper cmds) {
		mixerState = mixer.muteAll();
		JudahZone.getMetronome().setVolume(0);
		queue.push(cmds);
	}

	void queue(CommandWrapper command) {
		if (command != null)
			queue.push(command);
	}
	
	void internal(String name, String param) {
		if ("AndILoveHer".equals(name)) 
			_andILoveHer();
		if ("FeelGoodInc".equals(name))
			_feelGoodInc();
		if ("IFeelLove".equals(name))
			_iFeelLove();
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
			Console.warn(e.getMessage() + " for " + trig, e);
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
				clock.end();
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
		// if (clock != null) clock.removeListener(this);
		pulse = 8;
		Metronome.remove(this);
		control = ControlMode.EXTERNAL;
		clock.end();
		clock.removeListener(this);
		drums.addListener(this);
		clock.setLength(drums.getRecordedLength(), drums.getSize());
		Console.info("internal: _andILoveHer()");

	}

	private void _feelGoodInc() {
		Recorder drums = (Recorder)mixer.getSamples().get(0);
		clock.setLength(drums.getRecordedLength(), drums.getSize());
	}

	private void _iFeelLove() {
		
	}

		
}

//void setTempo(float tempo2) {
//if (tempo2 < tempo || tempo2 > tempo) {
//	tempo = tempo2;
//	if (JudahZone.getMetronome() != null)
//		JudahZone.getMetronome().setTempo(tempo2);
//}
//}

//TODO handle dropDaBeat		
//if (c.getCommand() == commands.dropBeat) { 
//	Console.warn("UN-QUEUEU!!");
//	try { // TODO metronome.unMute();
//		mixer.restoreState(mixerState);
//	} catch (JudahException e) {
//		log.error(e.getMessage(), e);
//		Console.warn("restore state: " + e.getMessage());
//	}}
//else commander.fire(c.getCommand(), c.getProps(), -1);}

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
///** external clock */
//void pulse() {
//	count += pulse;
//	Console.addText("-- beat: " + count + " vs. " + active.getTimestamp());
//	while (active.go(count)) {
//		execute(active);
//		increment();
//	}
//	while (queue.isEmpty() == false) {
//		CommandPair c = queue.pop();
//		if (c.getCommand() == commands.dropBeat) { 
//			Console.warn("UN-QUEUEU!!");
//			try {// metronome.unMute(); // TODO
//				mixer.restoreState(mixerState);
//			} catch (JudahException e) {
//				log.error(e.getMessage(), e);
//				Console.warn("restore state: " + e.getMessage());
//			}
//		}
//		else
//			commander.fire(c.getCommand(), c.getProps(), -1);
//	}//}
///** external clock */
//@Override // ControlChange Listener deprecate? see TimeListener    
//public void controlChange(ShortMessage event) {
//	if (event.getData1() != 3) 
//		return;
//	if (isRunning()) 
//		pulse();
//}
