package net.judah.sequencer;

import static net.judah.settings.Commands.SequencerLbls.*;
import static net.judah.util.Constants.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import org.jaudiolibs.jnajack.JackTransportState;

import net.judah.api.Command;
import net.judah.api.TimeListener.Property;
import net.judah.mixer.MixerCommands;
import net.judah.sequencer.Sequencer.ControlMode;
import net.judah.util.CommandPair;
import net.judah.util.Console;
import net.judah.util.Constants;

public class SeqCommands extends ArrayList<Command> {

	/** beats before Transport starts */
	public static final String PARAM_INTRO = "intro.beats";
	/** total beats to play */
	public static final String PARAM_DURATION = "duration.beats";
	/** note_on note, first beat of bar (optional) */
	public static final String PARAM_DOWNBEAT = "midi.downbeat";
	/** note_on note (optional) */
	public static final String PARAM_BEAT = "midi.beat"; 

	final Command setup, sequence, active, volume, 
		trigger, clicktrack, transport, dropBeat, 
		external, internal, queue, reload;

	SeqCommands(final Sequencer seq) {
		trigger = new Command(TRIGGER.name, TRIGGER.desc) {
			@Override public void execute(HashMap<String, Object> props, int midiData2) throws Exception {
				if (midiData2 == 0) 
					seq.trigger();
			}};

		clicktrack = new Command(CLICKTRACK.name, CLICKTRACK.desc, clicktrackTemplate()) {
			@Override public void execute(HashMap<String, Object> props, int midiData2) throws Exception {

				Integer intro = null;
				Integer duration = null;
				int down = 34; 
				int up = 33;
				try { intro = Integer.parseInt(props.get(PARAM_INTRO).toString());
				} catch (Throwable e) { 
					Console.warn(e.getMessage() + " " + PARAM_INTRO + " = " + props.get(PARAM_INTRO)); 
				}
				try { duration = Integer.parseInt(props.get(PARAM_DURATION).toString());
				} catch (Throwable e) {
					Console.warn(e.getMessage() + " " + PARAM_DURATION + " = " + props.get(PARAM_DURATION));
				}
				try { 
					down = Integer.parseInt(props.get(PARAM_DOWNBEAT).toString());
					up = Integer.parseInt(props.get(PARAM_BEAT).toString());
				} catch (Throwable e) { }
				if (intro != null && duration != null) {
					SeqClock clock = new SeqClock(seq, intro, duration, down, up);
					seq.setClock(clock);
				}
				//	void setup(Integer intro, Integer duration) throws InvalidMidiDataException, OSCSerializeException, IOException {
				//	seq.setup(intro, duration);
			}};

		setup = new Command(SETUP.name, SETUP.desc, settings()) {
			@Override public void execute(HashMap<String, Object> props, int midiData2) throws Exception {
				seq.setClock(new SeqClock(seq, props));
			}
		};
		sequence = new SeqCmd();
		active = new Command(ACTIVE.name, ACTIVE.desc, activeTemplate()) {
			@Override public void execute(HashMap<String, Object> props, int midiData2) throws Exception {
				boolean active = false;
				if (midiData2 >= 0) 
					active = midiData2 > 0;
				else 
					active = Boolean.parseBoolean("" + props.get(PARAM_ACTIVE));
				String[] names =  props.get(PARAM_NAME).toString().split(",");
				for (String name : names) {
					SeqData data = seq.getClock().byName(name);
					if (data == null) 
						Console.warn("Unknown named sequence: " + props.get(PARAM_NAME));
					else
						data.setActive(active);
				}
			}
			
		};
			
		volume = new Command(VOLUME.name, VOLUME.desc, gainTemplate()) {
			@Override public void execute(HashMap<String, Object> props, int midiData2) throws Exception {
				float gain = 0f;
				if (midiData2 >= 0)
					gain = midiData2 / 100f;
				else 
					gain = Float.parseFloat("" + props.get(PARAM_GAIN));
				String[] names =  props.get(PARAM_NAME).toString().split(",");
				for (String name : names) {
					SeqData data = seq.getClock().byName(name);
					if (data == null) 
						Console.warn("Unknown named sequence: " + props.get(PARAM_NAME));
					else
						data.setVolume(gain);
					}
			}
			
		};
		
		transport = new Command(TRANSPORT.name, TRANSPORT.desc, Constants.active()) {
			@Override public void execute(HashMap<String, Object> props, int midiData2) throws Exception {
				boolean active = false;
				if (midiData2 < 0) 
					try { active = Boolean.parseBoolean("" + props.get(PARAM_ACTIVE));
					} catch (Throwable t) { Console.warn(t.getMessage());}
				else if (midiData2 > 0) 
					active = true;
				if (active) seq.update(Property.TRANSPORT, JackTransportState.JackTransportStarting); 
				else seq.update(Property.TRANSPORT, JackTransportState.JackTransportStopped);}};				
				
		external = new Command(EXTERNAL.name, EXTERNAL.desc, externalParams()) {
			@Override public void execute(HashMap<String, Object> props, int midiData2) throws Exception {
				seq.externalControl(props);}};
		internal = new Command(INTERNAL.name, INTERNAL.desc, internalParams()) {
			@Override public void execute(HashMap<String, Object> props, int midiData2) {
				seq.internal("" + props.get(Sequencer.PARAM_PATCH), "" + props.get("param"));}};
		dropBeat = new Command(DROPBEAT.name, DROPBEAT.desc) {
			@Override public void execute(HashMap<String, Object> props, int midiData2) throws Exception {
				seq.dropDaBeat(new CommandPair(this, null));}};
		queue = new Command(QUEUE.name, QUEUE.desc, queueTemplate()) {
			@Override public void execute(HashMap<String, Object> props, int midiData2) throws Exception {
				Command target = seq.getCommander().find("" + props.get("command"));
				if (target == null) throw new NullPointerException("command " + props.get("command"));
				int candidate = seq.getCount() + 1;
				if (ControlMode.INTERNAL == seq.getControl()) {
					while (candidate % seq.getMeasure() != 0) 
						candidate++;
				}
				props.put(Sequencer.PARAM_SEQ_INTERNAL, candidate);
				seq.queue(new CommandPair(target, props));
			}};
				
		reload = new Command(RELOAD.name, RELOAD.desc) {
			@Override public void execute(HashMap<String, Object> props, int midiData2) throws Exception {
				seq.getPage().reload();}};
				
		addAll(Arrays.asList(new Command[] {
				setup, transport, sequence, active, volume,
				trigger, external, internal, 
				dropBeat, queue, reload, clicktrack 	
		}));
	}


	private HashMap<String, Class<?>> gainTemplate() {
		HashMap<String, Class<?>> result = new HashMap<>();
		result.put(PARAM_NAME, String.class);
		result.put(PARAM_GAIN, Float.class);
		return result;
	}

	private HashMap<String, Class<?>> activeTemplate() {
		HashMap<String, Class<?>> result = Constants.active();
		result.put(PARAM_NAME, String.class);
		return result;
	}


	// tempo // bpb // type // intro // pulse
	public static HashMap<String, Class<?>> settings() {
		HashMap<String, Class<?>> result = new HashMap<>();
		result.put(Constants.PARAM_BPM, Float.class);
		result.put(Constants.PARAM_MEASURE, Integer.class);
		result.put(PARAM_INTRO, Integer.class);
		result.put(Sequencer.PARAM_PULSE, Integer.class);
		return result;
	}

	public static HashMap<String, Class<?>> queueTemplate() {
		HashMap<String, Class<?>> result = MixerCommands.loopProps();
		result.put("command", String.class);
		return result;
	}

	public static HashMap<String, Class<?>> internalParams() {
		HashMap<String, Class<?>> result = new HashMap<>(2);
		result.put(Sequencer.PARAM_PATCH, String.class);
		result.put("param", String.class);
		return result;
	}
	
	public static HashMap<String, Class<?>> externalParams() {
		HashMap<String, Class<?>> result = new HashMap<>(2);
		result.put(Sequencer.PARAM_LOOP, Integer.class);
		result.put(Sequencer.PARAM_PULSE, Integer.class);
		return result;
	}

	
	
	public static HashMap<String, Class<?>> clicktrackTemplate() {
		HashMap<String, Class<?>> params = new HashMap<String, Class<?>>();
		
		params.put(PARAM_INTRO, Integer.class); 
		params.put(PARAM_DURATION, Integer.class); 
		params.put(PARAM_DOWNBEAT, Integer.class);
		params.put(PARAM_BEAT, Integer.class);
		//params.put(Constants.PARAM_CHANNEL, Integer.class);
		//params.put(PARAM_MIDIFILE, String.class); 
		return params;
	}

	
}

//unit = new Command(UNIT.name, UNIT.desc, unitParams()) {
//@Override public void execute(HashMap<String, Object> props, int midiData2) throws Exception {
//	seq.setUnit(Sequencer.TimeBase.valueOf("" + props.get("timeUnit")));}};

/*public void execute(Command cmd, HashMap<String, Object> props) throws Exception {
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
	}*/